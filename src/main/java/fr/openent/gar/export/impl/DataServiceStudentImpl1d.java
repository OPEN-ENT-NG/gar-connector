package fr.openent.gar.export.impl;

import fr.openent.gar.export.DataService;
import fr.openent.gar.helper.impl.PaginatorHelperImpl;
import fr.openent.gar.helper.impl.XmlExportHelperImpl;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.entcore.common.utils.StringUtils;

import static fr.openent.gar.constants.GarConstants.*;
import static fr.openent.gar.export.impl.DataServiceStudentImpl.*;

public class DataServiceStudentImpl1d extends DataServiceBaseImpl implements DataService {
    private final PaginatorHelperImpl paginator;
    private final String entId;
    private final String source;

    DataServiceStudentImpl1d(String entId, String source, JsonObject config, String strDate) {
        this.entId = entId;
        this.source = source;
        xmlExportHelper = new XmlExportHelperImpl(entId, source, config, STUDENT_ROOT, STUDENT_FILE_PARAM, strDate);
        paginator = new PaginatorHelperImpl();
    }

    /**
     * Export Data to folder
     * - Export Students identities
     * - Export Students Mefs
     *
     * @param handler response handler
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {
        final JsonArray mefs = new fr.wseduc.webutils.collections.JsonArray();
        getAndProcessStudentsInfo(0, mefs, studentsResult -> {
            if (validResponse(studentsResult, handler)) {
                if (validResponse(processStudentsMefs(mefs), handler)) {
                    xmlExportHelper.closeFile();
                    handler.handle(new Either.Right<>(
                            new JsonObject().put(
                                    FILE_LIST_KEY,
                                    xmlExportHelper.getFileList()
                            )));
                }
            }
        });
    }

    /**
     * Process students info, validate data and save to xml
     *
     * @param handler result handler
     */
    private void getAndProcessStudentsInfo(int skip, JsonArray mefs, final Handler<Either<String, JsonObject>> handler) {
        getStudentsInfoFromNeo4j(skip, entId, this.source, paginator, studentResults -> {
            if (validResponseNeo4j(studentResults, handler)) {
                final JsonArray students = studentResults.right().getValue();
                populateMef(mefs, students);
                Either<String, JsonObject> result = processStudentsInfo(students);
                if (studentResults.right().getValue().size() == PaginatorHelperImpl.LIMIT) {
                    getAndProcessStudentsInfo(skip + PaginatorHelperImpl.LIMIT, mefs, handler);
                } else {
                    handler.handle(result);
                }
            } else {
                log.error("[DataServiceStudentImpl@getAndProcessStudentsInfo] Failed to process");
            }
        });
    }

    private void populateMef(JsonArray mefs, final JsonArray students) {
        if (!students.isEmpty()) {
            students.forEach(student -> {
                if (student instanceof JsonObject) {
                    final JsonObject fields = (JsonObject) student;
                    populateMef(fields, mefs);
                }
            });
        }
    }

    private void populateMef(final JsonObject fields, JsonArray mefs) {
        final String userLevel = StringUtils.trimToNull(fields.getString("level"));
        final JsonArray uais = fields.getJsonArray("profiles");
        //export not empty Mef
        if (userLevel != null) {
            final String[] elems = userLevel.split("\\$");
            if (elems.length > 1) {
                final String mefCode = StringUtils.trimToNull(elems[0]);
                if (mefCode != null && uais != null && !uais.isEmpty()) {
                    uais.forEach(uai -> {
                        if (uai instanceof String) {
                            final String strUai = (String) uai;
                            if (StringUtils.trimToNull(strUai) != null) {
                                final JsonObject jo = new JsonObject();
                                jo.put(STRUCTURE_UAI, strUai);
                                jo.put(PERSON_ID, fields.getString(PERSON_ID));
                                jo.put(MEF_CODE_1D, mefCode);
                                mefs.add(jo);
                            }
                        }
                    });
                }
            }
        }
    }

    /**
     * Process mefs info
     *
     * @param mefs Array of mefs from Neo4j
     */
    private Either<String, JsonObject> processStudentsMefs(JsonArray mefs) {
        Either<String, JsonObject> event = processSimpleArray(mefs, PERSON_MEF_1D, PERSON_MEF_NODE_MANDATORY_1D);
        if (event.isLeft()) {
            return new Either.Left<>("Error when processing students mefs : " + event.left().getValue());
        } else {
            return event;
        }
    }

    /**
     * Process students info
     * Add structures in arrays to match xsd
     *
     * @param students Array of students from Neo4j
     */
    private Either<String, JsonObject> processStudentsInfo(JsonArray students) {
        try {
            for (Object o : students) {
                if (!(o instanceof JsonObject)) continue;
                JsonObject student = (JsonObject) o;
                JsonArray profiles = student.getJsonArray("profiles", null);
                if (profiles == null || profiles.size() == 0) {
                    log.warn("Gar : Student with no profile for export, id "
                            + student.getString("u.id", "unknown"));
                    continue;
                }

                processProfilesStudent(student);

                if (isMandatoryFieldsAbsent(student, STUDENT_NODE_MANDATORY)) {
                    log.warn("Gar : mandatory attribut for Student : " + student);
                    continue;
                }
                reorganizeNodes(student);
                xmlExportHelper.saveObject(STUDENT_NODE, student);
            }
            return new Either.Right<>(null);
        } catch (Exception e) {
            return new Either.Left<>("Error when processing students Info : " + e.getMessage());
        }
    }
}
