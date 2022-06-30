package fr.openent.gar.export.impl;

import fr.openent.gar.Gar;
import static fr.openent.gar.Gar.config;
import fr.openent.gar.constants.GarConstants;
import fr.openent.gar.export.ExportService;
import fr.openent.gar.export.XMLValidationHandler;
import fr.openent.gar.service.TarService;
import fr.openent.gar.service.impl.DefaultTarService;
import fr.openent.gar.utils.FileUtils;
import fr.wseduc.webutils.Either;
import fr.wseduc.webutils.data.FileResolver;
import fr.wseduc.webutils.email.EmailSender;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.impl.BufferImpl;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.email.EmailFactory;
import org.entcore.common.http.request.JsonHttpServerRequest;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ExportImpl {

    private final Logger log = LoggerFactory.getLogger(ExportImpl.class);
    private ExportService exportService;
    private TarService tarService;
    private JsonObject sftpGarConfig;
    private EventBus eb;
    private Vertx vertx;
    private final EmailSender emailSender;

    public ExportImpl(Vertx vertx, String entId, String source, Handler<String> handler) {
        this.vertx = vertx;
        this.eb = vertx.eventBus();
        this.exportService = new ExportServiceImpl(config);
        this.tarService = new DefaultTarService();
        this.sftpGarConfig = config.getJsonObject("gar-sftp");
        this.emailSender = new EmailFactory(vertx, config).getSender();
        if (Gar.AAF1D.equals(source)) {
            this.exportAndSend1d(entId, source, handler);
        } else {
            this.exportAndSend2d(entId, source, handler);
        }
    }

    public ExportImpl(Vertx vertx) {
        this.emailSender = new EmailFactory(vertx, config).getSender();
    }

    private void exportAndSend1d(final String entId, final String source, final Handler<String> handler) {
        final String exportPath = FileUtils.appendPath(config.getString("export-path"), entId + GarConstants.EXPORT_1D_SUFFIX);
        final String exportArchivePath = FileUtils.appendPath(config.getString("export-archive-path"), entId + GarConstants.EXPORT_1D_SUFFIX);
        exportAndSend(entId, source, handler, exportPath, exportArchivePath);
    }

    private void exportAndSend2d(final String entId, final String source, final Handler<String> handler) {
        final String exportPath = FileUtils.appendPath(config.getString("export-path"), entId);
        final String exportArchivePath = FileUtils.appendPath(config.getString("export-archive-path"), entId);
        exportAndSend(entId, source, handler, exportPath, exportArchivePath);
    }

    private void exportAndSend(String entId, final String source, Handler<String> handler, String exportPath, String exportArchivePath) {
        //create and delete files if necessary
        FileUtils.mkdirs(exportPath);
        FileUtils.deleteFiles(exportPath);
        FileUtils.mkdirs(exportArchivePath);
        FileUtils.deleteFiles(exportArchivePath);

        log.info("Start exportAndSend GAR for ENT ID : " + entId + " " + source +
                "(Generate xml files, XSD validation, compress to tar.gz, generate md5, send to GAR by sftp");
        log.info("Generate XML files");
        exportService.launchExport(entId, source, (Either<String, JsonObject> event1) -> {
            if (event1.isRight()) {
                File directory = new File(exportPath);
                Map<String, Object> validationResult = validateXml(directory, source);
                boolean isValid = (boolean) validationResult.get("valid");
                if (!isValid) {
                    log.info(validationResult.get("report"));
                    saveXsdValidation((String) validationResult.get("report"), FileUtils.appendPath(exportArchivePath, "xsd_errors.log"));
                    sendReport((String) validationResult.get("report"));
                }
                log.info("Tar.GZ to Compress");
                tarService.compress(exportArchivePath, directory, (Either<String, JsonObject> event2) -> {
                    if (event2.isRight() && event2.right().getValue().containsKey("archive")) {
                        String archiveName = event2.right().getValue().getString("archive");
                        //SFTP sender
                        log.info("Send to GAR tar GZ by sftp: " + archiveName);

                        final JsonObject tenant = sftpGarConfig.getJsonObject("tenants", new JsonObject()).getJsonObject(entId, new JsonObject());

                        JsonObject sendTOGar = new JsonObject().put("action", "send")
                                .put("known-hosts", sftpGarConfig.getString("known-hosts"))
                                .put("hostname", sftpGarConfig.getString("host"))
                                .put("port", sftpGarConfig.getInteger("port"))
                                .put("username", tenant.getString("username"))
                                .put("sshkey", tenant.getString("sshkey"))
                                .put("passphrase", tenant.getString("passphrase"))
                                .put("local-file", FileUtils.appendPath(exportArchivePath, archiveName))
                                .put("dist-file", FileUtils.appendPath(tenant.getString("dir-dest"), archiveName));

                        String n = (String) vertx.sharedData().getLocalMap("server").get("node");
                        String node = (n != null) ? n : "";

                        eb.send(node + "sftp", sendTOGar, new DeliveryOptions().setSendTimeout(300 * 1000L),
                                handlerToAsyncHandler((Message<JsonObject> messageResponse) -> {
                            if (messageResponse.body().containsKey("status") && messageResponse.body().getString("status").equals("error")) {
                                String e = "Send to GAR tar GZ by sftp but received an error : " +
                                        messageResponse.body().getString("message");
                                log.error(e);
                                handler.handle(e);
                            } else {
                                String md5File = event2.right().getValue().getString("md5File");
                                log.info("Send to GAR md5 by sftp: " + md5File);
                                sendTOGar
                                        .put("local-file", FileUtils.appendPath(exportArchivePath, md5File))
                                        .put("dist-file", FileUtils.appendPath(tenant.getString("dir-dest"), md5File));
                                eb.send(node + "sftp", sendTOGar, handlerToAsyncHandler(message1 -> {
                                    if (message1.body().containsKey("status") && message1.body().getString("status").equals("error")) {
                                        String e = "FAILED Send to Md5 by sftp : " +
                                                messageResponse.body().getString("message");
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
                        String e = "Failed Export and Send to GAR, tar service : failure in compressing the files => " +
                                event2.left().getValue();
                        log.error(e);
                        handler.handle(e);
                    }
                });
            } else {
                String e = "Failed Export and Send to GAR export service : failure during exporting the data => " +
                        event1.left().getValue();
                log.error(e);
                handler.handle(e);
            }
        });
    }

    public void sendReport(String report) {
        JsonArray recipients = config.getJsonArray("xsd-recipient-list", new JsonArray());
        String subject = "[GAR][" + config.getString("host") + "] XSD Validation error";
        for (int i = 0; i < recipients.size(); i++) {
            try{
                final HttpServerRequest request = new JsonHttpServerRequest(new JsonObject());
                String recipient = recipients.getString(i);
                emailSender.sendEmail(request, recipient, null, null, subject, report, null, false, null);
            }catch(Exception e){
                log.error("Failed to send report: ", e);
            }
        }
    }

    private void saveXsdValidation(String report, final String filePath) {
        vertx.fileSystem().writeFile(filePath, new BufferImpl().setBytes(0, report.getBytes()), event -> {
            if (event.failed()) log.error("Failed to write xsd errors");
        });
    }

    private Map<String, Object> validateXml(File directory, String source) {
        Map<String, Object> result = new HashMap<>();
        XMLValidationHandler errorHandler = new XMLValidationHandler();
        try {
            SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            String schemaPath = FileResolver.absolutePath("public/xsd");
            final Schema schema;
            if (Gar.AAF1D.equals(source)) {
                schema = factory.newSchema(new File(schemaPath + "/GAR-ENT-1D.xsd"));
            } else {
                schema = factory.newSchema(new File(schemaPath + "/GAR-ENT.xsd"));
            }

            Validator validator = schema.newValidator();
            validator.setErrorHandler(errorHandler);
            String[] files = directory.list();
            if (files == null) {
                result.put("valid", true);
                return result;
            }
            for (String f : files) {
                File currentFile = new File(directory.getPath(), f);
                validator.validate(new StreamSource(currentFile));
            }
        } catch (SAXException | IOException e) {
            log.error("Error while validating xml", e);
            result.put("valid", false);
        } finally {
            result.put("valid", errorHandler.isValid());
            result.put("report", errorHandler.report());
        }
        return result;
    }
}