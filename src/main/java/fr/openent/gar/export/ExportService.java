package fr.openent.gar.export;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

public interface ExportService {
    void launchExport(final String entId, final String source, final Handler<Either<String,JsonObject>> handler);
}
