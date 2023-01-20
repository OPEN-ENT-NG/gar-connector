package fr.openent.gar.export.impl;

import fr.openent.gar.export.DataService;
import fr.openent.gar.helper.impl.PaginatorHelperImpl;
import fr.openent.gar.helper.impl.XmlExportHelperImpl;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

import static fr.openent.gar.constants.GarConstants.*;
import static fr.openent.gar.export.impl.DataServiceTeacherImpl.*;

public class DataServiceTeacherImpl1d extends DataServiceBaseImpl implements DataService {
    private final PaginatorHelperImpl paginator;
    private final String entId;
    private final String source;

    DataServiceTeacherImpl1d(String entId, String source, JsonObject config, String strDate) {
        this.entId = entId;
        this.source = source;
        xmlExportHelper = new XmlExportHelperImpl(entId, source, config, TEACHER_ROOT, TEACHER_FILE_PARAM, strDate);
        paginator = new PaginatorHelperImpl();
    }

    /**
     * Export Data to folder
     * - Export Teachers identities
     * - Export Teachers Mefs
     *
     * @param handler response handler
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {
        //fixme : no modules IN 1D, perhaps level in futur like student
        final JsonArray modules = new fr.wseduc.webutils.collections.JsonArray();
        getAndProcessTeachersInfoFromNeo4j(0, modules, resultTeachers -> {
            if (validResponse(resultTeachers, handler)) {
                if (validResponse(processTeachersMefs(modules), handler)) {
                    xmlExportHelper.closeFile();
                    handler.handle(new Either.Right<>(
                            new JsonObject().put(
                                    FILE_LIST_KEY,
                                    xmlExportHelper.getFileList()
                            )));
                } else {
                    log.error("[DataServiceTeacherImpl1d@exportData] Failed to process");
                }
            } else {
                log.error("[DataServiceTeacherImpl1d@exportData] Failed to process");
            }
        });
    }

    private void getAndProcessTeachersInfoFromNeo4j(int skip, JsonArray modules,
                                                    final Handler<Either<String, JsonObject>> handler) {
        getTeachersInfoFromNeo4j(skip, this.source, entId, paginator, teacherInfos -> {
            if (validResponseNeo4j(teacherInfos, handler)) {
                final JsonArray teachers = teacherInfos.right().getValue();
                populateModules(modules, teachers, true);
                Either<String, JsonObject> result = processTeachersInfo(teachers);

                if (teacherInfos.right().getValue().size() == PaginatorHelperImpl.LIMIT) {
                    getAndProcessTeachersInfoFromNeo4j(skip + PaginatorHelperImpl.LIMIT, modules, handler);
                } else {
                    handler.handle(result);
                }
            } else {
                log.error("[DataServiceTeacherImpl1d@getAndProcessTeachersInfoFromNeo4j] Failed to process");
            }
        });
    }

    /**
     * Process teachers info
     * Add structures in arrays to match xsd
     * @param teachers Array of teachers from Neo4j
     */
    private Either<String, JsonObject> processTeachersInfo(JsonArray teachers) {
        try {
            for(Object o : teachers) {
                if(!(o instanceof JsonObject)) continue;

                JsonObject teacher = (JsonObject) o;
                JsonArray profiles = teacher.getJsonArray("UAIprofiles", null);
                if(profiles == null || profiles.size() == 0) {
                    log.error("Gar : Teacher with no profile or function for export, id "
                            + teacher.getString("u.id", "unknown"));
                    continue;
                }

                Map<String,String> userStructProfiles = new HashMap<>();

                processFunctions(teacher, userStructProfiles, true);
                processTeacherProfiles(teacher, userStructProfiles);

                if(isMandatoryFieldsAbsent(teacher, TEACHER_NODE_MANDATORY)) {
                    log.warn("Gar : mandatory attribut for Teacher : " + teacher);
                    continue;
                }
                //invalid xsd with men:GARPersonMail for 1D
                teacher.remove(PERSON_MAIL);

                reorganizeNodes(teacher);

                xmlExportHelper.saveObject(TEACHER_NODE, teacher);
            }
            return new Either.Right<>(null);
        } catch (Exception e) {
            log.error("Error when processing teachers Info : ", e.getMessage());
            throw e;
        }
    }

    /**
     * Process mefs info
     *
     * @param mefs Array of mefs from Neo4j
     */
    private Either<String, JsonObject> processTeachersMefs(JsonArray mefs) {
        Either<String, JsonObject> event = processSimpleArray(mefs, PERSON_MEF_1D, PERSON_MEF_NODE_MANDATORY_1D);
        if (event.isLeft()) {
            return new Either.Left<>("Error when processing teacher mefs : " + event.left().getValue());
        } else {
            return event;
        }
    }
}
