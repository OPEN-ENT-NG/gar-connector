package fr.openent.gar.controller;

import fr.openent.gar.Gar;
import fr.openent.gar.constants.Field;
import fr.openent.gar.constants.ManualExamples;
import fr.openent.gar.export.impl.ExportWorker;
import fr.openent.gar.constants.ExternalResourceExamples;
import fr.openent.gar.security.WorkflowUtils;
import fr.openent.gar.service.EventService;
import fr.openent.gar.service.ParameterService;
import fr.openent.gar.service.ResourceService;
import fr.openent.gar.service.impl.DefaultEventService;
import fr.openent.gar.service.impl.DefaultParameterService;
import fr.openent.gar.service.impl.DefaultResourceService;
import fr.wseduc.bus.BusAddress;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.user.UserUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;
import static fr.wseduc.webutils.http.response.DefaultResponseHandler.defaultResponseHandler;

public class GarController extends ControllerHelper {

    private final ResourceService resourceService;
    private final EventService eventService;
    private final ParameterService parameterService;
    private final Logger log = LoggerFactory.getLogger(GarController.class);
    private final EventBus eb;
    private final JsonObject config;

    public GarController(Vertx vertx, JsonObject config) {
        super();
        eb = vertx.eventBus();
        this.config = config;
        this.eventService = new DefaultEventService(config.getString("event-collection", "gar-events"));
        this.resourceService = new DefaultResourceService(
                vertx,
                config.getJsonObject("gar-ressources")
        );
        this.parameterService = new DefaultParameterService(eb);
    }

    @Get("")
    @SecuredAction("gar.view")
    public void render(HttpServerRequest request) {
        renderView(request, new JsonObject());
    }

    @Get("/config")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    public void getConfig(final HttpServerRequest request) {
        JsonObject safeConfig = config.copy();

        JsonObject garSftp = safeConfig.getJsonObject("gar-sftp", null);
        if (garSftp != null) {
            JsonObject tenants = garSftp.getJsonObject("tenants", null);
            if(tenants != null) {
                for (Iterator<Map.Entry<String, Object>> it = tenants.stream().iterator(); it.hasNext(); ) {
                    JsonObject tenant = (JsonObject) it.next().getValue();
                    if (tenant.getString("passphrase", null) != null) tenant.put("passphrase", "**********");
                }
            }
        }

        renderJson(request, safeConfig);
    }

    @Get("/resources")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void getResources(final HttpServerRequest request) {
        UserUtils.getUserInfos(eb, request, user -> {
            String structureId = request.params().contains("structure") ? request.getParam("structure") : user.getStructures().get(0);
            String userId = user.getUserId();
            this.resourceService.get(userId, structureId, result -> {
                            if (result.isRight()) {
                                Renders.renderJson(request, result.right().getValue());
                            } else {
                                Renders.renderJson(request, new JsonArray());
                            }
                        }
            );
        });
    }

    @Post("/event")
    @SecuredAction(value = "", type = ActionType.AUTHENTICATED)
    public void postEvent(HttpServerRequest request) {
        RequestUtils.bodyToJson(request, pathPrefix + "event", body -> {
            UserUtils.getUserInfos(eb, request, user -> {
                eventService.add(body, user, defaultResponseHandler(request));
            });
        });
    }


    @Get("/launchExport/:source")
    @SecuredAction(value = WorkflowUtils.EXPORT, type = ActionType.WORKFLOW)
    public void launchExportFromRoute(HttpServerRequest request) {
        final String source = request.getParam("source").toUpperCase();
        if (Gar.AAF1D.equals(source) || Gar.AAF.equals(source)) {
            this.exportAndSend(config.getJsonObject("id-ent").getString(Renders.getHost(request)), source);
            request.response().setStatusCode(200).end("Import started");
        } else {
            badRequest(request);
        }
    }

    private void exportAndSend(final String entId, final String source) {
        final JsonObject param = new JsonObject().put("action", "exportAndSend").put("entId", entId).put("source", source);
        eb.request(ExportWorker.EXPORTWORKER_ADDRESS, param,
                handlerToAsyncHandler(event -> log.info("Export Gar Launched")));
    }

    @BusAddress(Gar.GAR_ADDRESS)
    public void addressHandler(Message<JsonObject> message) {
        String action = message.body().getString("action", "");
        switch (action) {
            case "export" : exportAndSend(null, null);
                break;
            case "getConfig":
                log.info("MEDIACENTRE GET CONFIG BUS RECEPTION");
                JsonObject data = (new JsonObject())
                        .put("status", "ok")
                        .put("message", config);
                message.reply(data);
                break;
            case "isInGarGroup":
                JsonArray structureIds = message.body().getJsonArray(Field.STRUCTURE_IDS);
                JsonObject params = new JsonObject()
                        .put(Field.STRUCTURE_IDS, structureIds)
                        .put(Field.USER_ID, message.body().getString(Field.USER_ID));

                this.parameterService.userHasGarGroup(params)
                        .onSuccess(result -> {
                            List<String> list = result.stream().map(e -> ((JsonObject) e).getString(Field.STRUCTURE_ID)).collect(Collectors.toList());
                            JsonObject res = new JsonObject();
                            structureIds.forEach(structureId -> res.put((String) structureId, list.contains(structureId)));
                            JsonObject response = new JsonObject()
                                    .put(Field.STATUS, Field.OK)
                                    .put(Field.MESSAGE, res);
                            message.reply(response);
                        }).onFailure(err -> {
                            JsonObject response = new JsonObject()
                                    .put(Field.STATUS, Field.KO)
                                    .put(Field.MESSAGE, err.getMessage());
                            message.reply(response);
                        });
                break;
            case "getResources":
                JsonObject body = message.body();
                String structureId = body.getString("structure");
                String userId = body.getString("user");

                this.resourceService.get(userId, structureId, result -> {
                    if (config.getBoolean(Field.DEV_DASH_MODE, false)) {
                        JsonArray garResources = new JsonArray();
                        if (result.isRight()) garResources.addAll(result.right().getValue());
                        if (config.getJsonArray(Field.DEMO_STRUCTURE_1D, new JsonArray()).contains(structureId)) {
                            garResources.addAll(new JsonArray(ExternalResourceExamples.GAR_EXTERNAL_RESOURCE_EXAMPLE_1D));
                        } else {
                            garResources.addAll(new JsonArray(ExternalResourceExamples.GAR_EXTERNAL_RESOURCE_EXAMPLE));
                            garResources.addAll(new JsonArray(ManualExamples.GAR_MANUAL_EXAMPLE));
                        }

                        JsonObject response = new JsonObject()
                                .put("status", "ok")
                                .put("message", garResources);
                        message.reply(response);
                    }
                    else if (result.isRight()) {
                        JsonObject response = new JsonObject()
                                .put("status", "ok")
                                .put("message", result.right().getValue());
                        message.reply(response);
                    }
                    else {
                        JsonObject response = new JsonObject()
                                .put("status", "ko")
                                .put("message", result.left().getValue());
                        message.reply(response);
                    }
                }
                );
                break;
            default:
                log.error("Gar invalid.action " + action);
                JsonObject json = (new JsonObject())
                        .put("status", "error")
                        .put("message", "invalid.action");
                message.reply(json);
        }
    }
}