package fr.openent.gar.export.impl;

import fr.openent.gar.Gar;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.utils.StringUtils;
import org.vertx.java.busmods.BusModBase;

import java.util.Calendar;
import java.util.List;

public class ExportWorker extends BusModBase implements Handler<Message<JsonObject>> {
    public static final String EXPORTWORKER_ADDRESS = "openent.exportworker";
    private ExportImpl export = null;
    private Calendar lastExportTime = Calendar.getInstance();


    @Override
    public void start() {
        super.start();
        vertx.eventBus().localConsumer(EXPORTWORKER_ADDRESS, this);
    }

    @Override
    public void handle(Message<JsonObject> message) {
        final String action = message.body().getString("action", "");
        if ("exportAndSend".equals(action)) {
            export(message.body().getString("entId"), message.body().getString("source"));
        }
    }

    private void export(final String entId, String source) {
        Calendar now = Calendar.getInstance();
        if(this.export == null || ( (now.getTimeInMillis() - lastExportTime.getTimeInMillis()) / 1000 / 3600) > 1 )  {
            this.lastExportTime = now;
            if (entId != null) {
                //default AAF
                source = (source == null) ? Gar.AAF : source;
                this.export = new ExportImpl(vertx, entId, source, s -> export = null);
            } else {
                export(0);
            }
        }
    }

    private void export(final int index) {
        //export for each entId
        final List ids = config.getJsonArray("entid-sources", new JsonArray()).getList();
        if (index < ids.size()) {
            final List<String> idSource = StringUtils.split((String)ids.get(index), "-");
            this.export = new ExportImpl(vertx, idSource.get(0), idSource.get(1), s -> export(index+1));
        } else {
            export = null;
        }
    }
}
