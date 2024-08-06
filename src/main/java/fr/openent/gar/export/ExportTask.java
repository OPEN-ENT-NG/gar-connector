package fr.openent.gar.export;

import fr.openent.gar.Gar;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import static fr.wseduc.webutils.Utils.handlerToAsyncHandler;

public class ExportTask implements Handler<Long> {
    private final EventBus eb;
    private final Logger log = LoggerFactory.getLogger(ExportTask.class);

    public ExportTask(EventBus eb) {
        this.eb = eb;
    }

    @Override
    public void handle(Long event) {
        log.info("export launched");
        eb.request(Gar.GAR_ADDRESS,
                new JsonObject().put("action", "export"),
                handlerToAsyncHandler(event1 -> {
                    if ("ok".equals(event1.body().getString("status"))) {
                        log.info("export succeeded");
                    }
                }));
    }
}
