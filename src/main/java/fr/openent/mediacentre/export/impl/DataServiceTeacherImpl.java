package fr.openent.mediacentre.export.impl;

import fr.openent.mediacentre.helper.impl.XmlExportHelperImpl;
import fr.openent.mediacentre.export.DataService;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.entcore.common.neo4j.Neo4jResult.validResultHandler;
import static fr.openent.mediacentre.constants.GarConstants.*;

public class DataServiceTeacherImpl extends DataServiceBaseImpl implements DataService{

    DataServiceTeacherImpl(JsonObject config, String strDate) {
        super(config);
        xmlExportHelper = new XmlExportHelperImpl(config, TEACHER_ROOT, TEACHER_FILE_PARAM, strDate);
    }

    /**
     * Export Data to folder
     * - Export Teachers identities
     * - Export Teachers Mefs
     * @param handler response handler
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {
        getTeachersInfoFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> resultTeachers) {
                if (validResponseNeo4j(resultTeachers, handler)) {

                    processTeachersInfo(resultTeachers.right().getValue());
                    getTeachersMefFromNeo4j(
                            new Handler<Either<String, JsonArray>>() {
                                @Override
                                public void handle(Either<String, JsonArray> mefsResult) {
                                    if (mefsResult.isLeft()) {
                                        handler.handle(new Either.Left<String, JsonObject>(mefsResult.left().getValue()));
                                    } else {
                                        processTeachersMefs(mefsResult.right().getValue());
                                        xmlExportHelper.closeFile();
                                        handler.handle(new Either.Right<String, JsonObject>(
                                                new JsonObject().put(
                                                        FILE_LIST_KEY,
                                                        xmlExportHelper.getFileList()
                                                )));
                                    }
                                }
                            });
                }
            }
        });
    }

    /**
     * Process teachers info, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessTeachersInfo(final Handler<Either<String, JsonObject>> handler) {

        getTeachersInfoFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> teacherInfos) {
                if( validResponseNeo4j(teacherInfos, handler) ) {
                    Either<String,JsonObject> result = processTeachersInfo( teacherInfos.right().getValue() );
                    handler.handle(result);
                }
            }
        });
    }

    /**
     * Process teachers mefs, validate data and save to xml
     * @param handler result handler
     */
    private void getAndProcessTeachersMefs(final Handler<Either<String, JsonObject>> handler) {

        getTeachersMefFromNeo4j(new Handler<Either<String, JsonArray>>() {
            @Override
            public void handle(Either<String, JsonArray> teacherMefs) {
                if( validResponseNeo4j(teacherMefs, handler) ) {
                    Either<String,JsonObject> result = processTeachersMefs( teacherMefs.right().getValue() );
                    handler.handle(result);
                }
            }
        });
    }

    /**
     * Get teachers infos from Neo4j
     * Set fields as requested by xsd, except for structures
     * @param handler results
     */
    private void getTeachersInfoFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "match (u:User)-[IN]->(pg:ProfileGroup)-[:DEPENDS]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}), " +
                "(p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup) " +
                "where p.name = 'Teacher' " +
                "OPTIONAL MATCH (u:User)-[ADMINISTRATIVE_ATTACHMENT]->(sr:Structure) ";
        String dataReturn = "return distinct u.id  as `" + PERSON_ID + "`, " +
                "u.lastName as `" + PERSON_PATRO_NAME + "`, " +
                "u.lastName as `" + PERSON_NAME + "`, " +
                "u.firstName as `" + PERSON_FIRST_NAME + "`, " +
                "coalesce(u.otherNames, [u.firstName]) as `" + PERSON_OTHER_NAMES + "`, " +
                //TODO GARPersonCivilite
                "sr.UAI as `" + PERSON_STRUCT_ATTACH + "`, " +
                "u.birthDate as `" + PERSON_BIRTH_DATE + "`, " +
                "u.functions as functions, " +
                "collect(distinct s.UAI) as profiles " +
                "order by " + "`" + PERSON_ID + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process teachers info
     * Add structures in arrays to match xsd
     * @param teachers Array of teachers from Neo4j
     */
    private Either<String,JsonObject> processTeachersInfo(JsonArray teachers) {
        try {
            for(Object o : teachers) {
                if(!(o instanceof JsonObject)) continue;

                JsonObject teacher = (JsonObject) o;
                JsonArray profiles = teacher.getJsonArray("profiles", null);
                if(profiles == null || profiles.size() == 0) {
                    log.error("Mediacentre : Teacher with no profile or function for export, id "
                            + teacher.getString("u.id", "unknown"));
                    continue;
                }

                Map<String,String> userStructProfiles = new HashMap<>();

                processProfiles(teacher, TEACHER_PROFILE, userStructProfiles);
                processFunctions(teacher, userStructProfiles);

                if(isMandatoryFieldsAbsent(teacher, TEACHER_NODE_MANDATORY)) {
                    log.warn("Mediacentre : mandatory attribut for Teacher : " + teacher);
                    continue;
                }

                reorganizeNodes(teacher);

                xmlExportHelper.saveObject(TEACHER_NODE, teacher);
            }
            return new Either.Right<>(null);
        } catch (Exception e) {
            return new Either.Left<>("Error when processing teachers Info : " + e.getMessage());
        }
    }

    /**
     * XSD specify precise order for xml tags
     * @param teacher
     */
    private void reorganizeNodes(JsonObject teacher) {
        JsonObject personCopy = teacher.copy();
        teacher.clear();
        teacher.put(PERSON_ID, personCopy.getValue(PERSON_ID));
        teacher.put(PERSON_PROFILES, personCopy.getValue(PERSON_PROFILES));
        teacher.put(PERSON_PATRO_NAME, personCopy.getValue(PERSON_PATRO_NAME));
        teacher.put(PERSON_NAME, personCopy.getValue(PERSON_NAME));
        teacher.put(PERSON_FIRST_NAME, personCopy.getValue(PERSON_FIRST_NAME));
        teacher.put(PERSON_OTHER_NAMES, personCopy.getValue(PERSON_OTHER_NAMES));
        //TODO GARPersonCivilite
        teacher.put(PERSON_STRUCT_ATTACH, personCopy.getValue(PERSON_STRUCT_ATTACH));
        teacher.put(PERSON_STRUCTURE, personCopy.getValue(PERSON_STRUCTURE));
        teacher.put(PERSON_BIRTH_DATE, personCopy.getValue(PERSON_BIRTH_DATE));
        teacher.put(TEACHER_POSITION, personCopy.getValue(TEACHER_POSITION));
    }

    /**
     * Process teachers functions
     * Calc profile for Documentalist functions
     * Teacher function is in form structID$functionCode$functionDesc$roleCode and must be splited
     * and analyzed
     * Documentalists have specific role and profile
     * @param teacher to process functions for
     * @param structMap map between structures ID and profile
     */
    private void processFunctions(JsonObject teacher, Map<String,String> structMap) {
        JsonArray functions = teacher.getJsonArray("functions", null);
        if(functions == null || functions.size() == 0) {
            return;
        }

        JsonArray garFunctions = new fr.wseduc.webutils.collections.JsonArray();
        for(Object o : functions) {
            if(!(o instanceof String)) continue;
            String[] arrFunction = ((String)o).split("\\$");
            if(arrFunction.length < 4) continue;
            String structID = arrFunction[0];
            if(!mapStructures.containsKey(structID)) {
                log.error("Mediacentre : Invalid structure for profile " + o);
                continue;
            }
            String structUAI = mapStructures.get(structID);
            String functionCode = arrFunction[1];
            String functionDesc = arrFunction[2];
            String roleCode = arrFunction[3];
            String profileType = TEACHER_PROFILE;
            if(DOCUMENTALIST_CODE.equals(functionCode) && DOCUMENTALIST_DESC.equals(functionDesc)) {
                profileType = DOCUMENTALIST_PROFILE;
            }
            structMap.put(structUAI, profileType);

            JsonObject function = new JsonObject();
            function.put(STRUCTURE_UAI, structUAI);
            function.put(POSITION_CODE, roleCode);
            garFunctions.add(function);
        }
        teacher.put(TEACHER_POSITION, garFunctions);
        teacher.remove("functions");
    }

    /**
     * Get teachers mefs from Neo4j
     * @param handler results
     */
    private void getTeachersMefFromNeo4j(Handler<Either<String, JsonArray>> handler) {
        String query = "MATCH  (p:Profile)<-[HAS_PROFILE]-(pg:ProfileGroup)<-[IN]-" +
                "(u:User)-[ADMINISTRATIVE_ATTACHMENT]->(s:Structure)" +
                "<-[:DEPENDS]-(g:Group{name:\"" + CONTROL_GROUP + "\"}) ";
        String dataReturn = "where p.name = 'Teacher' " +
                "with s,u unwind u.modules as module " +
                "return distinct "+
                "s.UAI as `" + STRUCTURE_UAI + "`, " +
                "u.id as `" + PERSON_ID + "`, " +
                "split(module,'$')[1] as `" + MEF_CODE + "` " +
                "order by " + "`" + PERSON_ID + "`";
        neo4j.execute(query + dataReturn, new JsonObject(), validResultHandler(handler));
    }

    /**
     * Process mefs info
     * @param mefs Array of mefs from Neo4j
     */
    private Either<String,JsonObject> processTeachersMefs(JsonArray mefs) {
        //processSimpleArray(mefs, PERSON_MEF, PERSON_MEF_NODE_MANDATORY);
        Either<String,JsonObject> event =  processSimpleArray(mefs, PERSON_MEF, PERSON_MEF_NODE_MANDATORY);
        if(event.isLeft()) {
            return new Either.Left<>("Error when processing teacher mefs : " + event.left().getValue());
        } else {
            return event;
        }
    }
}