package fr.openent.gar.service.impl;

import fr.openent.gar.Gar;
import fr.openent.gar.service.ResourceService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.net.PemKeyCertOptions;
import org.entcore.common.neo4j.Neo4j;
import org.entcore.common.neo4j.Neo4jResult;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public class DefaultResourceService implements ResourceService {

    private final Vertx vertx;
    private final String garHost;
    private final JsonObject idsEnt;
    private final JsonObject hostById;
    private final Logger log = LoggerFactory.getLogger(DefaultResourceService.class);
    private final Map<String, HttpClient> httpClientByDomain = new HashMap();

    public DefaultResourceService(Vertx vertx, JsonObject garRessource, JsonObject idsEnt) {
        this.vertx = vertx;
        this.garHost = garRessource.getString("host");
        this.idsEnt = idsEnt;

        final JsonObject hostById = new JsonObject();
        for (String hostName : idsEnt.fieldNames()) {
            hostById.put(idsEnt.getString(hostName), hostName);
        }
        this.hostById = hostById;

        final JsonObject domains =  garRessource.getJsonObject("domains", new JsonObject());

        for (String domain : domains.fieldNames()) {
            final JsonObject res = domains.getJsonObject(domain);
            if (res == null) continue;
            try {
                httpClientByDomain.put(domain, generateHttpClient(new URI(garHost), res.getString("cert"), res.getString("key")));
            } catch (URISyntaxException e) {
                log.error("[DefaultResourceService@constructor] An error occurred when creating the URI", e);
            }
        }
    }

    @Override
    public void get(String userId, String structure, String hostname, Handler<Either<String, JsonArray>> handler) {
        if(userId == null){
            handler.handle(new Either.Left<>("[DefaultResourceService@get] No userid." ));
            return;
        }
        if(structure == null){
            handler.handle(new Either.Left<>("[DefaultResourceService@get] No structure." ));
            return;
        }

        String uaiQuery = "MATCH (s:Structure {id: {structureId}}) return s.UAI as UAI, s.name as name, s.exports as exports";
        JsonObject params = new JsonObject().put("structureId", structure);

        Neo4j.getInstance().execute(uaiQuery, params, Neo4jResult.validResultHandler(event -> {
            if (event.isRight()) {
                JsonArray results = event.right().getValue();
                if (results.size() > 0) {
                    String uai = results.getJsonObject(0).getString("UAI");
                    String structureName = results.getJsonObject(0).getString("name");
                    String host = "";
                    if (Gar.demo) {
                        JsonObject resources = new JsonObject("{ \"listeRessources\": { \"ressource\": [ { \"idRessource\": \"http://n2t.net/ark:/99999/r14xxxxxxxx\", \"idType\": \"ARK\", \"nomRessource\": \"R14_ELEVE_RIEN - Manuel num??rique ??l??ve (100% num??rique) - Multisupports (tablettes + PC/Mac)\", \"idEditeur\": \"378901946_0000000000000000\", \"nomEditeur\": \"Worldline\", \"urlVignette\": \"https://vignette.validation.test-gar.education.fr/VAtest1/gar/152.png\", \"typePresentation\": { \"code\": \"MAN\", \"nom\": \"manuels num??riques\" }, \"typePedagogique\": [{ \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-010-num-006\", \"nom\": \"??tude de cas\" }], \"typologieDocument\": [{ \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-005-num-024\", \"nom\": \"livre num??rique\" }], \"niveauEducatif\": [], \"domaineEnseignement\": [{ \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2550\", \"nom\": \"des chr??tiens dans l\\u0027Empire (histoire 6e)\" }, { \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2551\", \"nom\": \"les relations de l\\u0027Empire romain avec la Chine des Han (histoire 6e)\" }, { \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-993\", \"nom\": \"l\\u0027Empire romain dans le monde antique (histoire 6e)\" }, { \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2544\", \"nom\": \"la ?? r??volution ?? n??olithique (histoire 6e)\" }, { \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-2549\", \"nom\": \"conqu??tes, paix romaine et romanisation (histoire 6e)\" }, { \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-990\", \"nom\": \"histoire (6e)\" }, { \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-991\", \"nom\": \"la longue histoire de l\\u0027humanit?? et des migrations (histoire 6e)\" }], \"urlAccesRessource\": \"https://sp-auth.validation.test-gar.education.fr/domaineGar?idENT\\u003dRU5UVEVTVDE\\u003d\\u0026idEtab\\u003dMDY1MDQ5OVAtRVQ2\\u0026idSrc\\u003daHR0cDovL24ydC5uZXQvYXJrOi85OTk5OS9yMTR4eHh4eHh4eA\\u003d\\u003d\", \"nomSourceEtiquetteGar\": \"Accessible via le Gestionnaire d???acc??s aux ressources (GAR)\", \"distributeurTech\": \"378901946_0000000000000000\", \"validateurTech\": \"378901946_0000000000000000\" }, { \"idRessource\": \"http://n2t.net/ark:/99999/r20xxxxxxxx\", \"idType\": \"ARK\", \"nomRessource\": \"R20_ELEVE_1SEUL - Manuel num??rique ??l??ve (100% num??rique) - Multisupports (tablettes + PC/Mac)\", \"idEditeur\": \"378901946_0000000000000000\", \"nomEditeur\": \"Worldline\", \"urlVignette\": \"https://vignette.validation.test-gar.education.fr/VAtest1/gar/114.png\", \"typePresentation\": { \"code\": \"MAN\", \"nom\": \"manuels num??riques\" }, \"typePedagogique\": [{ \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-010-num-027\", \"nom\": \"activit?? p??dagogique\" }, { \"uri\": \"http://data.education.fr/voc/scolomfr/concept/lecture\", \"nom\": \"cours / pr??sentation\" }], \"typologieDocument\": [{ \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-005-num-024\", \"nom\": \"livre num??rique\" }], \"niveauEducatif\": [], \"domaineEnseignement\": [{ \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-1357\", \"nom\": \"fran??ais (cycle 3)\" }], \"urlAccesRessource\": \"https://sp-auth.validation.test-gar.education.fr/domaineGar?idENT\\u003dRU5UVEVTVDE\\u003d\\u0026idEtab\\u003dMDY1MDQ5OVAtRVQ2\\u0026idSrc\\u003daHR0cDovL24ydC5uZXQvYXJrOi85OTk5OS9yMjB4eHh4eHh4eA\\u003d\\u003d\", \"nomSourceEtiquetteGar\": \"Accessible via le Gestionnaire d???acc??s aux ressources (GAR)\", \"distributeurTech\": \"378901946_0000000000000000\", \"validateurTech\": \"378901946_0000000000000000\" } , { \"idRessource\": \"http://n2t.net/ark:/99999/r14xxxxxxx2\", \"idType\": \"ARK\", \"nomRessource\": \"Physique Chimie\", \"idEditeur\": \"378901946_0000000000000000\", \"nomEditeur\": \"Micorom??ga - Hatier\", \"urlVignette\": \"https://vignette.validation.test-gar.education.fr/VAtest1/gar/113.png\", \"typePresentation\": { \"code\": \"MAN\", \"nom\": \"manuels num??riques\" }, \"typePedagogique\": [{ \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-010-num-006\", \"nom\": \"simulation\" }], \"typologieDocument\": [{ \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-005-num-024\", \"nom\": \"livre num??rique\" }], \"niveauEducatif\": [{ \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-022-num-023\", \"nom\": \"5e\" }], \"domaineEnseignement\": [ { \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-991\", \"nom\": \"Physique Chimie \" }], \"urlAccesRessource\": \"https://sp-auth.validation.test-gar.education.fr/domaineGar?idENT\\u003dRU5UVEVTVDE\\u003d\\u0026idEtab\\u003dMDY1MDQ5OVAtRVQ2\\u0026idSrc\\u003daHR0cDovL24ydC5uZXQvYXJrOi85OTk5OS9yMTR4eHh4eHh4eA\\u003d\\u003d\", \"nomSourceEtiquetteGar\": \"Accessible via le Gestionnaire d???acc??s aux ressources (GAR)\", \"distributeurTech\": \"378901946_0000000000000000\", \"validateurTech\": \"378901946_0000000000000000\" }, { \"idRessource\": \"http://n2t.net/ark:/99999/r20xxxxxxx2\", \"idType\": \"ARK\", \"nomRessource\": \"Arts Plastisque\", \"idEditeur\": \"378901946_0000000000000000\", \"nomEditeur\": \"C'est ?? voir\", \"urlVignette\": \"https://vignette.validation.test-gar.education.fr/VAtest1/gar/115.png\", \"typePresentation\": { \"code\": \"MAN\", \"nom\": \"manuels num??riques\" }, \"typePedagogique\": [{ \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-010-num-027\", \"nom\": \"activit?? p??dagogique\" }, { \"uri\": \"http://data.education.fr/voc/scolomfr/concept/lecture\", \"nom\": \"mat??riel de r??f??rence\" }], \"typologieDocument\": [{ \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-005-num-024\", \"nom\": \"livre num??rique\" }], \"niveauEducatif\": [], \"domaineEnseignement\": [{ \"uri\": \"http://data.education.fr/voc/scolomfr/concept/scolomfr-voc-015-num-1357\", \"nom\": \"fran??ais (cycle 3)\" }], \"urlAccesRessource\": \"https://sp-auth.validation.test-gar.education.fr/domaineGar?idENT\\u003dRU5UVEVTVDE\\u003d\\u0026idEtab\\u003dMDY1MDQ5OVAtRVQ2\\u0026idSrc\\u003daHR0cDovL24ydC5uZXQvYXJrOi85OTk5OS9yMjB4eHh4eHh4eA\\u003d\\u003d\", \"nomSourceEtiquetteGar\": \"Accessible via le Gestionnaire d???acc??s aux ressources (GAR)\", \"distributeurTech\": \"378901946_0000000000000000\", \"validateurTech\": \"378901946_0000000000000000\" } ] } }");
                        beautifyRessourcesResult(handler, uai, structureName, resources);
                    } else {
                        if (hostname == null) {
                            JsonArray exports = results.getJsonObject(0).getJsonArray("exports");
                            if (exports != null && !exports.isEmpty()) {
                                for (Object export : exports.getList()) {
                                    if (!(export instanceof String)) continue;
                                    String exp = (String) export;
                                    if (exp.contains("GAR-")) {
                                        String idEnt = exp.split("-")[1];
                                        host = hostById.getString(idEnt);
                                        break;
                                    }
                                }
                            }
                        } else {
                            host = hostname;
                        }

                        if(!idsEnt.containsKey(host)){
                            handler.handle(new Either.Left<>("[DefaultResourceService@get] This hostname is undefined in " +
                                    "config key id-ent, or hostname isn't match real hostname : " + host ));
                            return;
                        }

                        String garHostNoProtocol;
                        try {
                            URL url = new URL(garHost);
                            garHostNoProtocol = url.getHost();
                        } catch (Exception e) {
                            handler.handle(new Either.Left<>("[DefaultResourceService@get] Bad gar host url : " + garHost));
                            return;
                        }
                        String resourcesUri = garHost + "/ressources/" + idsEnt.getString(host) + "/" + uai + "/" + userId;
                        final HttpClient httpClient = httpClientByDomain.get(host);
                        if (httpClient == null) {
                            log.error("no gar ressources httpClient available for this host : " + host);
                            handler.handle(new Either.Left<>("[DefaultResourceService@get] " +
                                    "No gar ressources httpClient available for this host : " + host));
                            return;
                        }
                        final HttpClientRequest clientRequest = httpClient.get(resourcesUri, response -> {
                                    if (response.statusCode() != 200) {
                                        log.error("try to call " + resourcesUri);
                                        log.error(response.statusCode() + " " + response.statusMessage());

                                        response.bodyHandler(errBuff -> {
                                            JsonObject error = new JsonObject(new String(errBuff.getBytes()));
                                            if (error.containsKey("Erreur")) {
                                                handler.handle(new Either.Left<>(
                                                        error.getJsonObject("Erreur").getString("Message")));
                                            } else {
                                                handler.handle(new Either.Left<>("[DefaultResourceService@get] " +
                                                        "failed to connect to GAR servers: " + response.statusMessage()));
                                            }
                                        });
                                    } else {
                                        Buffer responseBuffer = new BufferImpl();
                                        response.handler(responseBuffer::appendBuffer);
                                        response.endHandler(aVoid -> {
                                            JsonObject resources = new JsonObject(decompress(responseBuffer));
                                            beautifyRessourcesResult(handler, uai, structureName, resources);
                                        });
                                        response.exceptionHandler(throwable ->
                                                handler.handle(new Either.Left<>("[DefaultResourceService@get] " +
                                                        "failed to get GAR response: " + throwable.getMessage())));
                                    }
                                }).putHeader("Accept", "application/json")
                                .putHeader("Accept-Encoding", "gzip, deflate")
                                .putHeader("Host", garHostNoProtocol)
                                .putHeader("Cache-Control", "no-cache")
                                .putHeader("Date", new Date().toString());

                        clientRequest.end();
                    }
                } else{
                    handler.handle(new Either.Right<>(new JsonArray()));
                }
            } else {
                String message = "[DefaultResourceService@get] An error occurred when fetching structure UAI " +
                        "for structure " + structure;
                log.error(message);
                handler.handle(new Either.Left<>(message));
            }
        }));
    }

    private void beautifyRessourcesResult(Handler<Either<String, JsonArray>> handler, String uai, String structureName,
                                          JsonObject resources) {
        JsonArray ressourcesResult = resources.getJsonObject("listeRessources").getJsonArray("ressource");
        for (Object ressourceO : ressourcesResult) {
            JsonObject ressource = (JsonObject) ressourceO;
            ressource.put("structure_name", structureName);
            ressource.put("structure_uai", uai);
        }
        handler.handle(new Either.Right<>(ressourcesResult));
    }

    private String decompress(Buffer buffer) {
        StringBuilder output = new StringBuilder();
        try {
            GZIPInputStream gzipInputStream = new GZIPInputStream(new ByteArrayInputStream(buffer.getBytes()));
            BufferedReader bf = new BufferedReader(new InputStreamReader(gzipInputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = bf.readLine()) != null) {
                output.append(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return output.toString();
    }

    private HttpClient generateHttpClient(final URI uri, final String certPath, final String keyPath) {
        HttpClientOptions options = new HttpClientOptions()
                .setDefaultHost(uri.getHost())
                .setDefaultPort("https".equals(uri.getScheme()) ? 443 : 80)
                .setVerifyHost(false)
                .setTrustAll(true)
                .setSsl("https".equals(uri.getScheme()))
                .setKeepAlive(true)
                .setPemKeyCertOptions(getPemKeyCertOptions(certPath, keyPath));
        return vertx.createHttpClient(options);
    }

    private PemKeyCertOptions getPemKeyCertOptions(String certPath, String keyPath) {
        return new PemKeyCertOptions()
                .setCertPath(certPath)
                .setKeyPath(keyPath);
    }
}
