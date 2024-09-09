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
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

public class DefaultResourceService implements ResourceService {

    private final Vertx vertx;
    private final String garHost;
    private final Logger log = LoggerFactory.getLogger(DefaultResourceService.class);
    private final Map<String, HttpClient> httpClientByIdENT = new HashMap();

    public DefaultResourceService(Vertx vertx, JsonObject garRessource) {
        this.vertx = vertx;
        this.garHost = garRessource.getString("host");

        final JsonObject tenants = garRessource.getJsonObject("tenants", new JsonObject());

        List<String> idsENT = tenants.fieldNames().stream().filter(Objects::nonNull).collect(Collectors.toList());
        for (String idENT : idsENT) {
            final JsonObject tenant = tenants.getJsonObject(idENT);
            try {
                httpClientByIdENT.put(idENT, generateHttpClient(new URI(garHost), tenant.getString("cert"), tenant.getString("key")));
            } catch (URISyntaxException e) {
                log.error("[DefaultResourceService@constructor] An error occurred when creating the URI : " + e);
            }
        }
    }

    @Override
    public void get(String userId, String structure, Handler<Either<String, JsonArray>> handler) {
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
                    JsonArray exports = results.getJsonObject(0).getJsonArray("exports");
                    String idEnt = null;

                    if (exports != null && !exports.isEmpty()) {
                        for (Object export : exports.getList()) {
                            if (!(export instanceof String)) continue;
                            String exp = (String) export;
                            if (exp.contains("GAR-")) {
                                idEnt = exp.split("-")[1];
                                break;
                            }
                        }
                    }

                    if(idEnt == null){
                        handler.handle(new Either.Left<>("[DefaultResourceService@get] This structure has undefined " +
                                "gar id project in exports field : " + structure ));
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
                    String resourcesUri = garHost + "/ressources/" + idEnt + "/" + uai + "/" + userId;
                    log.info("[GAR@DefaultResourceService::get] " + resourcesUri);
                    final HttpClient httpClient = httpClientByIdENT.get(idEnt);
                    if (httpClient == null) {
                        log.error("no gar ressources httpClient available for this entId : " + idEnt);
                        handler.handle(new Either.Left<>("[DefaultResourceService@get] " +
                                "No gar ressources httpClient available for this entId : " + idEnt));
                        return;
                    }
                    final HttpClientRequest clientRequest = httpClient.get(resourcesUri, response -> {
                                if (response.statusCode() != 200) {
                                    log.error("try to call " + resourcesUri);
                                    log.error(response.statusCode() + " " + response.statusMessage());

                                    response.bodyHandler(errBuff -> {
                                        try {
                                            JsonObject error = new JsonObject(new String(errBuff.getBytes()));
                                            if (error.containsKey("Erreur")) {
                                                handler.handle(new Either.Left<>(
                                                        error.getJsonObject("Erreur").getString("Message")));
                                            } else {
                                                handler.handle(new Either.Left<>("[DefaultResourceService@get] " +
                                                        "failed to connect to GAR servers: " + response.statusMessage()));
                                            }
                                        } catch (Exception e) {
                                            handler.handle(new Either.Left<>("[DefaultResourceService@get] " +
                                                    "failed to connect to GAR servers: " + response.statusMessage()));
                                        }
                                    });
                                } else {
                                    Buffer responseBuffer = new BufferImpl();
                                    response.handler(responseBuffer::appendBuffer);
                                    response.endHandler(aVoid -> {
                                        JsonObject resources = new JsonObject(decompress(responseBuffer));
                                        log.info("[GAR@DefaultResourceService::get] " + resources);
                                        beautifyRessourcesResult(handler, uai, structureName, resources);
                                        log.info("[GAR@DefaultResourceService::get] " + resources);
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
                } else{
                    log.info("[GAR@DefaultResourceService::get] Result size <= 0");
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
                .setDefaultPort(uri.getPort() == -1 ? ("https".equals(uri.getScheme()) ? 443 : 80) : uri.getPort())
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
