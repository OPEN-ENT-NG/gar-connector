package fr.openent.gar.controller;

import fr.openent.gar.constants.Field;
import fr.openent.gar.service.impl.DefaultParameterService;
import fr.wseduc.rs.Get;
import fr.wseduc.rs.Post;
import fr.wseduc.webutils.http.Renders;
import fr.wseduc.webutils.request.RequestUtils;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import org.entcore.common.controller.ControllerHelper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;

public class DevController extends ControllerHelper {
    private final static String defaultResources = "{\n" +
            "   \"listeRessources\":{\n" +
            "      \"ressource\":[\n" +
            "         {\n" +
            "            \"idRessource\":\"http://n2t.net/ark:/99999/r20xxxxxxx%uai%\",\n" +
            "            \"idType\":\"ARK\",\n" +
            "            \"nomRessource\":\"Arts Plastisque %uai%\",\n" +
            "            \"idEditeur\":\"378901946_0000000000000000\",\n" +
            "            \"nomEditeur\":\"C'est Ã  voir\",\n" +
            "            \"urlVignette\":\"https://vignette.validation.test-gar.education.fr/VAtest1/gar/115.png\",\n" +
            "            \"typePresentation\":{\n" +
            "               \"code\":\"MAN\",\n" +
            "               \"nom\":\"manuels numériques\"\n" +
            "            },\n" +
            "            \"typePedagogique\":[\n" +
            "               {\n" +
            "                  \"uri\":\"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-010-num-027\",\n" +
            "                  \"nom\":\"activité pédagogique\"\n" +
            "               },\n" +
            "               {\n" +
            "                  \"uri\":\"http://data.education.fr/voc/scolomfr/concept/lecture\",\n" +
            "                  \"nom\":\"matériel de référence\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"typologieDocument\":[\n" +
            "               {\n" +
            "                  \"uri\":\"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-005-num-024\",\n" +
            "                  \"nom\":\"livre numérique\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"niveauEducatif\":[\n" +
            "               \n" +
            "            ],\n" +
            "            \"domaineEnseignement\":[\n" +
            "               {\n" +
            "                  \"uri\":\"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-1357\",\n" +
            "                  \"nom\":\"français (cycle 3)\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"urlAccesRessource\":\"https://sp-auth.validation.test-gar.education.fr/domaineGar?idENT=RU5UVEVTVDE=&idEtab=MDY1MDQ5OVAtRVQ2&idSrc=aHR0cDovL24ydC5uZXQvYXJrOi85OTk5OS9yMjB4eHh4eHh4eA==\",\n" +
            "            \"nomSourceEtiquetteGar\":\"Accessible via le Gestionnaire d'accès aux ressources (GAR)\",\n" +
            "            \"distributeurTech\":\"378901946_0000000000000000\",\n" +
            "            \"validateurTech\":\"378901946_0000000000000000\"\n" +
            "         },\n" +
            "\t\t {\n" +
            "            \"idRessource\":\"http://n2t.net/ark:/99999/r14xxxxxxx%uai%\",\n" +
            "            \"idType\":\"ARK\",\n" +
            "            \"nomRessource\":\"Educ'ARTE %uai%\",\n" +
            "            \"idEditeur\":\"378901946_0000000000000000\",\n" +
            "            \"nomEditeur\":\"Micoroméga - Hatier\",\n" +
            "            \"urlVignette\":\"https://vignette.gar.education.fr/VAprod/gar/8602.png\",\n" +
            "            \"typePresentation\":{\n" +
            "               \"code\":\"OTHER\",\n" +
            "               \"nom\":\"manuels numériques\"\n" +
            "            },\n" +
            "            \"typePedagogique\":[\n" +
            "               {\n" +
            "                  \"uri\":\"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-010-num-006\",\n" +
            "                  \"nom\":\"simulation\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"typologieDocument\":[\n" +
            "               {\n" +
            "                  \"uri\":\"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-005-num-024\",\n" +
            "                  \"nom\":\"banque de vidéos\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"niveauEducatif\":[\n" +
            "               {\n" +
            "                  \"uri\":\"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-022-num-023\",\n" +
            "                  \"nom\":\"5e\"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"domaineEnseignement\":[\n" +
            "               {\n" +
            "                  \"uri\":\"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-991\",\n" +
            "                  \"nom\":\"Physique Chimie \"\n" +
            "               }\n" +
            "            ],\n" +
            "            \"urlAccesRessource\":\"https://sp-auth.validation.test-gar.education.fr/domaineGar?idENT=RU5UVEVTVDE=&idEtab=MDY1MDQ5OVAtRVQ2&idSrc=aHR0cDovL24ydC5uZXQvYXJrOi85OTk5OS9yMTR4eHh4eHh4eA==\",\n" +
            "            \"nomSourceEtiquetteGar\":\"Accessible via le Gestionnaire d'accès aux ressources (GAR)\",\n" +
            "            \"distributeurTech\":\"378901946_0000000000000000\",\n" +
            "            \"validateurTech\":\"378901946_0000000000000000\"\n" +
            "         }\n" +
            "      ]\n" +
            "   }\n" +
            "}";

    Map<String, JsonObject> garResponseMap = new HashMap<>();
    Map<String, Integer> garResponseStatusMap = new HashMap<>();

    public DevController(Vertx vertx, JsonObject config) {
        Set<String> key = config.getJsonObject(Field.ID_DASH_ENT).fieldNames();
        if (!key.isEmpty()) {
            String idEnt = config.getJsonObject(Field.ID_DASH_ENT).getString(key.stream().findFirst().orElse(null));
            new DefaultParameterService(vertx.eventBus()).getDeployedStructureGar(idEnt)
                    .onSuccess(neo4jStructures -> neo4jStructures.forEach(neo4jStructure -> {
                        garResponseMap.put(idEnt + neo4jStructure.getUai(), new JsonObject(defaultResources.replace("%uai%", neo4jStructure.getUai())));
                        garResponseStatusMap.put(idEnt + neo4jStructure.getUai(), 200);
                    }));
        }
    }

    // Protect by dev mode. Not by active in pro
    @Get("/dev/gar/ressources/:idEnt/:uai/:userId")
    public void getGarResponse(final HttpServerRequest request) throws IOException {
        String idEnt = request.params().get(Field.IDENT);
        String uai = request.params().get(Field.UAI);
        int status = garResponseStatusMap.get(idEnt + uai);

        if (status >= 400 && status < 600) {
            Renders.render(request, new JsonObject(), status);
            return;
        }

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(outputStream);
        gzipOutputStream.write(garResponseMap.get(idEnt + uai).encodePrettily().getBytes(StandardCharsets.UTF_8));
        gzipOutputStream.close();

        // Renvoyer la réponse en tant que flux Gzip
        request.response().putHeader("Content-Encoding", "gzip");
        request.response().setStatusCode(status);
        request.response().end(Buffer.buffer(outputStream.toByteArray()));
    }

    // Protect by dev mode. Not by active in pro
    @Post("/dev/gar/ressources/:idEnt/:uai")
    public void setGarResponse(final HttpServerRequest request) {
        RequestUtils.bodyToJson(request, event -> {
            String idEnt = request.params().get(Field.IDENT);
            String uai = request.params().get(Field.UAI);
            garResponseMap.put(idEnt + uai, event.getJsonObject(Field.DATA));
            garResponseStatusMap.put(idEnt + uai, event.getInteger(Field.STATUS));
            Renders.renderJson(request, event);
        });
    }
}
