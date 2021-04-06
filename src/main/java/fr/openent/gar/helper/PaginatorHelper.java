package fr.openent.gar.helper;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface PaginatorHelper {
    void neoStreamList(String query, JsonObject params, JsonArray finalValues, int skip, Handler<Either<String, JsonArray>> handler);
    void neoStream(String query, JsonObject params, int skip, Handler<Either<String, JsonArray>> handler);
}
