package fr.openent.gar.model;

import fr.openent.gar.constants.Field;
import fr.openent.gar.helper.IModelHelper;
import io.vertx.core.json.JsonObject;

public class Neo4jStructure implements IModel<Neo4jStructure> {
    private String uai;

    public Neo4jStructure(JsonObject jsonObject) {
        this.uai = jsonObject.getString(Field.UAI);
    }

    @Override
    public JsonObject toJson() {
        return IModelHelper.toJson(this, true, true);
    }

    public String getUai() {
        return uai;
    }

    public Neo4jStructure setUai(String uai) {
        this.uai = uai;
        return this;
    }
}
