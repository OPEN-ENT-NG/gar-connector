package fr.openent.gar.export.impl;

import fr.openent.gar.Gar;
import fr.openent.gar.export.DataService;
import fr.openent.gar.export.ExportService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ExportServiceImpl implements ExportService{

    private final JsonObject config;
    private final Logger log = LoggerFactory.getLogger(ExportServiceImpl.class);

    public ExportServiceImpl(JsonObject config) {
        this.config = config;
    }

    @Override
    public void launchExport(final String entId, final String source, final Handler<Either<String, JsonObject>> handler) {

        String strDate = new SimpleDateFormat("yyyyMMdd_HHmmss_").format(new Date());

        final Queue<DataService> dataServiceQueue = new ConcurrentLinkedQueue<>();

        switch (source) {
            case Gar.AAF1D:
                dataServiceQueue.add(new DataServiceStructureImpl1d(entId, source, config, strDate));
                dataServiceQueue.add(new DataServiceStudentImpl1d(entId, source, config, strDate));
                dataServiceQueue.add(new DataServiceTeacherImpl1d(entId, source, config, strDate));
                dataServiceQueue.add(new DataServiceGroupImpl1d(entId, source, config, strDate));
                dataServiceQueue.add(new DataServiceRespImpl(entId, source, config, strDate));
                break;
            case Gar.AAF:
                dataServiceQueue.add(new DataServiceStructureImpl(entId, source, config, strDate));
                dataServiceQueue.add(new DataServiceStudentImpl(entId, source, config, strDate));
                dataServiceQueue.add(new DataServiceTeacherImpl(entId, source, config, strDate));
                dataServiceQueue.add(new DataServiceGroupImpl(entId, source, config, strDate));
                dataServiceQueue.add(new DataServiceRespImpl(entId, source, config, strDate));
                break;
            default:
                log.error("Invalid source : " + source);
        }

        JsonArray fileList = new fr.wseduc.webutils.collections.JsonArray();
        processExport(dataServiceQueue, fileList, handler);
    }

    private void processExport(final Queue<DataService> dataServiceQueue,
                               final JsonArray fileList,
                               final Handler<Either<String, JsonObject>> handler) {
        DataService service = dataServiceQueue.poll();
        if(service != null) {
            service.exportData(exportResult -> {
                if(exportResult.isRight()) {

                    JsonArray fileListResult = exportResult.right().getValue().getJsonArray(DataService.FILE_LIST_KEY);
                    if(fileListResult != null) {
                        for (Object o : fileListResult) {
                            fileList.add(o);
                        }
                    }
                    processExport(dataServiceQueue, fileList, handler);
                } else {
                    handler.handle(exportResult);
                }
            });
        } else {
            doReporting(fileList, handler);
        }
    }

    private void doReporting(final JsonArray fileList, final Handler<Either<String, JsonObject>> handler) {

        if(fileList.size() == 0) {
            handler.handle(new Either.Left<>("No file created"));
        } else  {
            JsonObject msgContent = new JsonObject();
            msgContent.put("path", fileList);
            //TODO faire le zip
            msgContent.put("zipfile", "");
            handler.handle(new Either.Right<>(new JsonObject()));
        }

    }
}
