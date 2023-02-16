package fr.openent.gar.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;

public interface ResourceService {

    /**
     * Get user GAR resources
     *
     * @param userId    User id
     * @param structure User structure
     * @param handler   Function handler returning data
     */
    void get(String userId, String structure, Handler<Either<String, JsonArray>> handler);
}
