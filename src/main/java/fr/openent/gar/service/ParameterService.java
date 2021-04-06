package fr.openent.gar.service;

import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface ParameterService {

    /**
     * Undeploy given structure.
     * Note that it does not delete GAR-RESP-AFFECT manual group. As resp are choose by structure, we
     * need to save it for a future deployment.
     *
     * @param structureId Structure identifier
     * @param entId Ent identifier
     * @param handler     Function handler returning data
     */
    void undeployStructureGar(String structureId, String entId, Handler<Either<String, JsonObject>> handler);

    /**
     * Get Structure with optional gar group
     * @param entId Ent identifier
     * @param handler Function handler returning data
     */
    void getStructureGar(String entId, Handler<Either<String, JsonArray>> handler);

    /**
     * Create new group gar to chosen structure
     * @param body          body query
     * @param result        Function handler returning data
     */
    void createGarGroupToStructure(JsonObject body, Handler<Either<String, JsonObject>> result);

    /**
     * Add specific user to gar group selected
     * @param body          body query
     * @param result        Function handler returning data
     */
    void addUserToGarGroup(JsonObject body, Handler<Either<String, JsonObject>> result);
}
