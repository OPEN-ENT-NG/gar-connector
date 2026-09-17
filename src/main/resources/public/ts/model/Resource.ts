import { _ } from "entcore";
import { Mix, Eventer } from "entcore-toolkit";
import { Type, Types, Helper, Structure, Event } from "./index";
import { TYPES } from "../definitions";
import { http } from 'entcore-toolkit';

export class Resource {
  idRessource: string;
  idType: string;
  nomRessource: string;
  idEditeur: string;
  nomEditeur: string;
  urlVignette: string;
  typePresentation: { code: string; nom: string };
  typePedagogique: Type[];
  typologieDocument: Type[];
  niveauEducatif: Type[];
  domaineEnseignement: Type[];
  urlAccesRessource: string;
  nomSourceEtiquetteGar: string;
  distributeurTech: string;
  validateurTech: string;

  open() {
    const tab = window.open(this.urlAccesRessource, "_blank");
    tab.focus();
    const event: Event = new Event(this);
    event.save();
  }
}

export class Resources {
  all: Resource[];
  eventer: Eventer;
  levels: Types;
  teachingFields: Types;
  educationalTypes: Types;
  typologies: Types;

  constructor() {
    this.all = [];
    this.eventer = new Eventer();
    this.levels = new Types();
    this.teachingFields = new Types();
    this.educationalTypes = new Types();
    this.typologies = new Types();
  }


    async sync(structure: Structure): Promise<void> {
    //TODO Call https methods. Use a provider to get data
    this.eventer.trigger("loading", { loading: true });
    console.log(
      `Loading resources for structure ${structure.getId()} - ${structure.getName()}`
    );

    let url = `/gar/resources?structure=${structure.getId()}`;
    let {data} = await http.get(url);

    this.all = Mix.castArrayAs(Resource, data);
    this.levels.all = Mix.castArrayAs(Type, this.getValues(TYPES.level));
    this.teachingFields.all = Mix.castArrayAs(
      Type,
      this.getValues(TYPES.teachingField)
    );
    this.educationalTypes.all = Mix.castArrayAs(
      Type,
      this.getValues(TYPES.educationalType)
    );
    this.typologies.all = Mix.castArrayAs(
      Type,
      this.getValues(TYPES.typologie)
    );
    this.educationalTypes.all = Mix.castArrayAs(
      Type,
      this.getValues(TYPES.educationalType)
    );
    this.eventer.trigger("loading", { loading: false });
  }

  /**
   * Aggregate values. Returns an array containing each value based provided key string
   * @param key Resource values key name. Should be typePedagogique, typologieDocument,
   * niveauEducatif or domaineEnseignement
   */
  getValues(key: string): Type[] {
    let map = {},
      levels = [];
    this.all.forEach((resource: Resource) => {
      resource[key].forEach((level: Type) => {
        if (!(level.nom in map)) {
          levels.push(level);
          map[level.nom] = true;
        }
      });
    });
    return levels;
  }

  /**
   * Filter Resource list based on filters parameters
   * @param filters filters list : { levels: Type[], teachingFields: Type[], educationalTypes: Type[], typologies: Type[] }
   */
  filter(filters): Resource[] {
    if (
      filters.levels.length === 0 &&
      filters.teachingFields.length === 0 &&
      filters.educationalTypes.length === 0 &&
      filters.typologies.length === 0
    )
      return this.all;

    const results: Resource[] = [];
    const levelsReg = Helper.generateRegexp(_.pluck(filters.levels, "nom"));
    const teachingFieldsReg = Helper.generateRegexp(
      _.pluck(filters.teachingFields, "nom")
    );
    const educationalTypesReg = Helper.generateRegexp(
      _.pluck(filters.educationalTypes, "nom")
    );
    const typologiesReg = Helper.generateRegexp(
      _.pluck(filters.typologies, "nom")
    );

    this.all.map((resource: Resource) => {
      let match: boolean = false;
      if (filters.levels.length > 0)
        resource.niveauEducatif.forEach(
          (type: Type) =>
            (match = match || levelsReg.test(type.nom.toLocaleLowerCase()))
        );

      if (filters.typologies.length > 0)
        resource.typologieDocument.forEach(
          (type: Type) =>
            (match = match || typologiesReg.test(type.nom.toLocaleLowerCase()))
        );

      if (filters.educationalTypes.length > 0)
        resource.typePedagogique.forEach(
          (type: Type) =>
            (match =
              match || educationalTypesReg.test(type.nom.toLocaleLowerCase()))
        );

      if (filters.teachingFields.length > 0)
        resource.domaineEnseignement.forEach(
          (type: Type) =>
            (match =
              match || teachingFieldsReg.test(type.nom.toLocaleLowerCase()))
        );

      if (match) results.push(resource);
    });
    return results;
  }
}
