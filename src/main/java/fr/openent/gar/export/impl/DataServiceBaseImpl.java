package fr.openent.gar.export.impl;

import fr.openent.gar.helper.XmlExportHelper;
import fr.openent.gar.export.DataService;
import fr.wseduc.webutils.Either;
import io.vertx.core.logging.LoggerFactory;
import org.entcore.common.neo4j.Neo4j;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;


import java.util.HashMap;
import java.util.Map;

import static fr.openent.gar.constants.GarConstants.*;

abstract class DataServiceBaseImpl implements DataService {
    static Map<String,String> mapStructures = new HashMap<>();
    XmlExportHelper xmlExportHelper;
    static final Logger log = LoggerFactory.getLogger(DataServiceBaseImpl.class);
    static final Neo4j neo4j = Neo4j.getInstance();

    DataServiceBaseImpl() {
    }

    /**
     * Save an array of JsonObjects in xml
     * Only save object with all mandatory fields present
     * @param array array from neo4j
     * @param name xml node name
     * @param mandatoryFields list of mandatory fields that must be present in the JsonObject
     * @return result
     */
    Either<String,JsonObject> processSimpleArray(JsonArray array, String name, String[] mandatoryFields) {
        try {

            for(Object o : array) {
                if (!(o instanceof JsonObject)) continue;
                JsonObject obj = (JsonObject)o;
                if(isMandatoryFieldsAbsent(obj, mandatoryFields)) {
                    log.warn(name + " object malformed : " + obj.toString());
                    continue;
                }
                xmlExportHelper.saveObject(name, obj);
            }
            return new Either.Right<>(null);
        } catch (Exception e){
            return new Either.Left<>(e.getMessage());
        }
    }

    static boolean isMandatoryFieldsAbsent(JsonObject obj, String[] mandatoryFields) {
        if(obj == null) return true;
        for(String s : mandatoryFields) {
            if(!obj.containsKey(s) || null == obj.getValue(s)) return true;
        }
        return false;
    }

    /**
     * Validate response and trigger handler if not
     * @param event event to validate
     * @param handler handler to respond to
     * @return true if response is valid
     */
    boolean validResponseNeo4j(Either<String,JsonArray> event, final Handler<Either<String, JsonObject>> handler) {
        if(event.isLeft()) {
            handler.handle(new Either.Left<>(event.left().getValue()));
            log.error(event.left().getValue());
            return false;
        } else {
            return true;
        }
    }

    /**
     * Validate response and trigger handler if not
     * @param event event to validate
     * @param handler handler to respond to
     * @return true if response is valid
     */
    boolean validResponse(Either<String,JsonObject> event, final Handler<Either<String, JsonObject>> handler) {
        if(event.isLeft()) {
            handler.handle(new Either.Left<>(event.left().getValue()));
            return false;
        } else {
            return true;
        }
    }

    /**
     * Add a profile in profileArray
     * @param profileArray Array to fill
     * @param structUAI structure UAI
     * @param profile profile name
     */
    protected static void addProfile(JsonArray profileArray, String structUAI, String profile) {
        JsonObject garProfile = new JsonObject();
        garProfile.put(STRUCTURE_UAI, structUAI);
        garProfile.put(PERSON_PROFILE, profile);
        profileArray.add(garProfile);
    }
}
