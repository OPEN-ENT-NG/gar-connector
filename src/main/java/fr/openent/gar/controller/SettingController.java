package fr.openent.gar.controller;

import fr.openent.gar.Gar;
import fr.openent.gar.constants.GarConstants;
import fr.openent.gar.export.impl.ExportImpl;
import fr.openent.gar.service.ParameterService;
import fr.openent.gar.service.impl.DefaultParameterService;
import fr.openent.gar.utils.FileUtils;
import fr.wseduc.rs.ApiDoc;
import fr.wseduc.rs.Delete;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.security.ActionType;
import fr.wseduc.security.SecuredAction;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;
import org.entcore.common.http.filter.ResourceFilter;
import org.entcore.common.http.filter.SuperAdminFilter;
import org.entcore.common.http.response.DefaultResponseHandler;

import java.util.List;

public class SettingController extends ControllerHelper {

    private final ParameterService parameterService;

    public SettingController(EventBus eb) {
        super();
        this.parameterService = new DefaultParameterService(eb);
    }
    /**
     * Access parameter url via url/mediacentre/parameter
     * Only access right
     */
    @Get("/parameter")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Render parameter view")
    public void setting(HttpServerRequest request) {
        renderView(request, null, "parameter.html", null);
    }

    @Get("/structure/gar")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("get gar structure")
    public void getStructureGar(final HttpServerRequest request) {
        final String entId = config.getJsonObject("id-ent").getString(Renders.getHost(request));
        parameterService.getStructureGar(entId, DefaultResponseHandler.arrayResponseHandler(request));
    }

    @Post("/structure/gar/group")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Create group to gar structure")
    public void createGarGroupToStructure(final HttpServerRequest request) {
        final String entId = config.getJsonObject("id-ent").getString(Renders.getHost(request));
        RequestUtils.bodyToJson(request, parameter -> {
            parameter.put("entId", entId);
            parameterService.createGarGroupToStructure(parameter, DefaultResponseHandler.defaultResponseHandler(request));
        });

    }

    @Delete("/structures/:id")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Undeploy given structure")
    public void undeployStructure(HttpServerRequest request) {
        final String entId = config.getJsonObject("id-ent").getString(Renders.getHost(request));
        String structureId = request.getParam("id");
        parameterService.undeployStructureGar(structureId, entId, DefaultResponseHandler.defaultResponseHandler(request));
    }

    @Post("/structure/gar/group/user")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Add user to gar group")
    public void addUserToGarGroup(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, parameter -> {
            parameterService.addUserToGarGroup(parameter, DefaultResponseHandler.defaultResponseHandler(request));
        });

    }

    @Get("/mail/test")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Test send mail")
    public void testMail(HttpServerRequest request) {
        ExportImpl export = new ExportImpl(vertx);
        export.sendReport("This is a test mail");
        ok(request);
    }

    @Get("/export/archive/:source")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Download last archive")
    public void downloadArchive(HttpServerRequest request) {
        final String entId = config.getJsonObject("id-ent").getString(Renders.getHost(request));
        final String source = request.getParam("source").toUpperCase();
        if (Gar.AAF1D.equals(source) || Gar.AAF.equals(source)) {
            final String archivePath;
            if (Gar.AAF1D.equals(source)) {
                archivePath = FileUtils.appendPath(config.getString("export-archive-path"), entId + GarConstants.EXPORT_1D_SUFFIX);
            } else {
                archivePath = FileUtils.appendPath(config.getString("export-archive-path"), entId);
            }
            vertx.fileSystem().readDir(archivePath, ".*\\.tar\\.gz", event -> {
                if (event.failed()) {
                    renderError(request, new JsonObject().put("error", event.cause().toString()));
                    return;
                }

                List<String> files = event.result();
                if (files.isEmpty()) notFound(request, "Archive not found");
                String filename = files.get(0).replace(archivePath, "");
                vertx.fileSystem().readFile(files.get(0), readEvent -> {
                    if (readEvent.failed()) {
                        renderError(request, new JsonObject().put("error", readEvent.cause().toString()));
                        return;
                    }

                    Buffer buff = readEvent.result();
                    request.response()
                            .putHeader("Content-Type", "application/gzip; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=" + filename)
                            .end(buff);
                });
            });
        } else {
            badRequest(request);
        }

    }

    @Get("/export/xsd/validation/:source")
    @SecuredAction(value = "", type = ActionType.RESOURCE)
    @ResourceFilter(SuperAdminFilter.class)
    @ApiDoc("Download xsd validation error")
    public void xsdValidation(HttpServerRequest request) {
        final String entId = config.getJsonObject("id-ent").getString(Renders.getHost(request));
        final String source = request.getParam("source").toUpperCase();
        if (Gar.AAF1D.equals(source) || Gar.AAF.equals(source)) {
            final String archivePath;
            if (Gar.AAF1D.equals(source)) {
                archivePath = FileUtils.appendPath(config.getString("export-archive-path"), entId + GarConstants.EXPORT_1D_SUFFIX);
            } else {
                archivePath = FileUtils.appendPath(config.getString("export-archive-path"), entId);
            }
            vertx.fileSystem().readDir(archivePath, "xsd_errors\\.log", event -> {
                if (event.failed()) {
                    renderError(request, new JsonObject().put("error", event.cause().toString()));
                    return;
                }

                List<String> files = event.result();
                if (files.isEmpty()) notFound(request, "File not found");
                String filename = "xsd_errors.log";
                vertx.fileSystem().readFile(files.get(0), readEvent -> {
                    if (readEvent.failed()) {
                        renderError(request, new JsonObject().put("error", readEvent.cause().toString()));
                        return;
                    }

                    Buffer buff = readEvent.result();
                    request.response()
                            .putHeader("Content-Type", "application/text; charset=utf-8")
                            .putHeader("Content-Disposition", "attachment; filename=" + filename)
                            .end(buff);
                });
            });
        } else {
            badRequest(request);
        }
    }
}
