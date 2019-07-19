package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.export.ExportService;
import fr.openent.mediacentre.service.TarService;
import fr.openent.mediacentre.service.impl.DefaultTarService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.io.File;

import static fr.openent.mediacentre.Mediacentre.CONFIG;
import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ExportImpl {

    private Logger log = LoggerFactory.getLogger(ExportImpl.class);
    private ExportService exportService;
    private TarService tarService;
    private JsonObject sftpGarConfig;
    private EventBus eb;
    private JsonObject config;
    private Vertx vertx;

    public ExportImpl(Vertx vertx, Handler<String> handler) {
        this.vertx = vertx;
        this.config = CONFIG;
        this.eb = vertx.eventBus();
        this.exportService = new ExportServiceImpl(config);
        this.tarService = new DefaultTarService();
        this.sftpGarConfig = config.getJsonObject("gar-sftp");

        this.exportAndSend(handler);
    }
    private void exportAndSend(Handler<String> handler) {
        log.info("Start exportAndSend GAR (Generate xml files, compress to tar.gz, generate md5, send to GAR by sftp");
        try {
            emptyDIrectory(config.getString("export-path"));
        } catch (Exception e) {
            handler.handle(e.getMessage());
        }
        log.info("Generate XML files");
        exportService.launchExport((Either<String, JsonObject> event1) -> {
            if (event1.isRight()) {
                File directory = new File(config.getString("export-path"));
                log.info("Tar.GZ to Compress");
                emptyDIrectory(config.getString("export-archive-path"));
                tarService.compress(config.getString("export-archive-path"), directory, (Either<String, JsonObject> event2) -> {
                    if (event2.isRight() && event2.right().getValue().containsKey("archive")) {
                        String archiveName = event2.right().getValue().getString("archive");
                        //SFTP sender
                        log.info("Send to GAR tar GZ by sftp: " + archiveName);
                        JsonObject sendTOGar = new JsonObject().put("action", "send")
                                .put("known-hosts", sftpGarConfig.getString("known-hosts"))
                                .put("hostname", sftpGarConfig.getString("host"))
                                .put("port", sftpGarConfig.getInteger("port"))
                                .put("username", sftpGarConfig.getString("username"))
                                .put("sshkey", sftpGarConfig.getString("sshkey"))
                                .put("passphrase", sftpGarConfig.getString("passphrase"))
                                .put("local-file", config.getString("export-archive-path") + archiveName)
                                .put("dist-file", sftpGarConfig.getString("dir-dest") + archiveName);

                        String n = (String) vertx.sharedData().getLocalMap("server").get("node");
                        String node = (n != null) ? n : "";

                        eb.send(node + "sftp", sendTOGar, handlerToAsyncHandler((Message<JsonObject> messageResponse) -> {
                            if (messageResponse.body().containsKey("status") && messageResponse.body().getString("status") == "error") {
                                String e = "Send to GAR tar GZ by sftp";
                                log.error(e);
                                handler.handle(e);
                            } else {
                                String md5File = event2.right().getValue().getString("md5File");
                                log.info("Send to GAR md5 by sftp: " + md5File);
                                sendTOGar
                                        .put("local-file", config.getString("export-archive-path") + md5File)
                                        .put("dist-file", sftpGarConfig.getString("dir-dest") + md5File);
                                eb.send(node + "sftp", sendTOGar, handlerToAsyncHandler(message1 -> {
                                    if (message1.body().containsKey("status") && message1.body().getString("status") == "error") {
                                        String e = "FAILED Send to Md5 by sftp";
                                        log.error(e);
                                        handler.handle(e);
                                    } else {
                                        log.info("SUCCESS Export and Send to GAR");
                                        handler.handle("SUCCESS");
                                    }
                                }));
                            }
                        }));
                    } else {
                        String e = "Failed Export and Send to GAR, tar service";
                        log.error(e);
                        handler.handle(e);
                    }
                });
            } else {
                String e = "Failed Export and Send to GAR export service";
                log.error(e);
                handler.handle(e);
            }
        });
    }

    //TODO prévenir les nullpointer ici
    private void emptyDIrectory(String path) {
        File index = new File(path);
        String[] entries = index.list();
        for (String s : entries) {
            File currentFile = new File(index.getPath(), s);
            currentFile.delete();
        }
    }

}
