import { angular } from "entcore";
import { http } from "entcore-toolkit";

import { Resource, Type } from "./index";

export class Event {
  idRessource: string;
  nomRessource: string;
  nomEditeur: string;
  urlVignette: string;
  typePedagogique: Type[];
  niveauEducatif: Type[];
  typePresentation: { code: string; nom: string };
  typologieDocument: Type[];
  domaineEnseignement: Type[];

  constructor({
    idRessource,
    nomRessource,
    nomEditeur,
    urlVignette,
    typePedagogique,
    niveauEducatif,
    typePresentation,
    typologieDocument,
    domaineEnseignement
  }: Resource) {
    this.idRessource = idRessource;
    this.nomRessource = nomRessource;
    this.nomEditeur = nomEditeur;
    this.urlVignette = urlVignette;
    this.typePedagogique = typePedagogique;
    this.niveauEducatif = niveauEducatif;
    this.typePresentation = typePresentation;
    this.typologieDocument = typologieDocument;
    this.domaineEnseignement = domaineEnseignement;
  }

  async save(): Promise<any> {
    return await http.post("/gar/event", this.toJSON());
  }

  toJSON() {
    return {
      idRessource: this.idRessource,
      nomRessource: this.nomRessource,
      nomEditeur: this.nomEditeur,
      urlVignette: this.urlVignette,
      typePedagogique: this.typePedagogique,
      niveauEducatif: JSON.parse(angular.toJson(this.niveauEducatif)),
      typePresentation: this.typePresentation,
      typologieDocument: this.typologieDocument,
      domaineEnseignement: this.domaineEnseignement
    };
  }
}
