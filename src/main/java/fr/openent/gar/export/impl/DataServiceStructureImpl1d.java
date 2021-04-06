package fr.openent.gar.export.impl;

import fr.openent.gar.export.DataService;
import fr.openent.gar.helper.impl.PaginatorHelperImpl;
import fr.openent.gar.helper.impl.XmlExportHelperImpl;
import fr.wseduc.webutils.Either;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import static fr.openent.gar.constants.GarConstants.*;
import static fr.openent.gar.constants.GarConstants.STRUCTURE_NODE;
import static fr.openent.gar.export.impl.DataServiceStructureImpl.getStucturesInfoFromNeo4j;
import static fr.openent.gar.export.impl.DataServiceStructureImpl.updateMap;

public class DataServiceStructureImpl1d extends DataServiceBaseImpl implements DataService {

    private final PaginatorHelperImpl paginator;
    private final String entId;
    private final String source;

    DataServiceStructureImpl1d(String entId, String source, JsonObject config, String strDate) {
        this.entId = entId;
        this.source = source;
        xmlExportHelper = new XmlExportHelperImpl(entId, source, config, STRUCTURE_ROOT, STRUCTURE_FILE_PARAM, strDate);
        paginator = new PaginatorHelperImpl();
    }

    /**
     * Export Data to folder
     * - Export Structures info and build mapStructures with mapping between structures ID and UAI
     * - Export Structures Mefs
     * - Export Structures fields of study
     */
    @Override
    public void exportData(final Handler<Either<String, JsonObject>> handler) {
        getAndProcessStructuresInfo(0, structInfoResults -> {
            if (validResponse(structInfoResults, handler)) {
                xmlExportHelper.closeFile();
                handler.handle(new Either.Right<>(
                        new JsonObject().put(
                                FILE_LIST_KEY,
                                xmlExportHelper.getFileList()
                        )));
            }
        });
    }

    /**
     * Process structure info, validate data and save to xml
     *
     * @param handler result handler
     */
    private void getAndProcessStructuresInfo(int skip, final Handler<Either<String, JsonObject>> handler) {
        getStucturesInfoFromNeo4j(skip, this.source, entId, paginator, structResults -> {
            if (validResponseNeo4j(structResults, handler)) {
                Either<String, JsonObject> result = processStructuresInfo(structResults.right().getValue());

                if (structResults.right().getValue().size() == PaginatorHelperImpl.LIMIT) {
                    getAndProcessStructuresInfo(skip + PaginatorHelperImpl.LIMIT, handler);
                } else {
                    handler.handle(result);
                }
            } else {
                log.error("[DataServiceStructureImpl1d@getAndProcessStructuresInfo] Failed to process");
            }
        });
    }

    /**
     * Process structures info
     * Update general map with mapping between structures ID and UAI
     *
     * @param structures Array of structures from Neo4j
     */
    private Either<String, JsonObject> processStructuresInfo(JsonArray structures) {
        try {
            //clean mapStructures before process structures.
            mapStructures.clear();
            for (Object o : structures) {
                if (!(o instanceof JsonObject)) continue;
                JsonObject structure = (JsonObject) o;

                if (isMandatoryFieldsAbsent(structure, STRUCTURE_NODE_MANDATORY)) continue;

                updateMap(structure);
                xmlExportHelper.saveObject(STRUCTURE_NODE, structure);
            }
            return new Either.Right<>(null);
        } catch (Exception e) {
            log.error("Error when processing structures Info : ", e);
            return new Either.Left<>("Error when processing structures Info : " + e.getMessage());
        }
    }
}
