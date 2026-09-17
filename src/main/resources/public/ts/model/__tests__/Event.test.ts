import { Event } from "../index";
import data from "../__mocks__/ressources";

import { http } from "entcore-toolkit";
import { mockHttpResponse } from "../../test-utils/httpMock";

jest.mock("entcore-toolkit", () => Object.assign({}, (jest as any).requireActual("entcore-toolkit"), {
  http: { get: jest.fn(), post: jest.fn(), put: jest.fn(), delete: jest.fn(), postFile: jest.fn(), putFile: jest.fn() }
}));

const resource = data.listeRessources.ressource[0];
const event = new Event(resource);

const mockedEvent = {
  idRessource: resource.idRessource,
  nomRessource: resource.nomRessource,
  nomEditeur: resource.nomEditeur,
  urlVignette: resource.urlVignette,
  typePedagogique: resource.typePedagogique,
  niveauEducatif: resource.niveauEducatif,
  typePresentation: resource.typePresentation,
  typologieDocument: resource.typologieDocument,
  domaineEnseignement: resource.domaineEnseignement
};

describe("constructor()", () => {
  it("should initialize Event with same information as the mock", () => {
    expect(event.idRessource).toBe(resource.idRessource);
    expect(event.nomRessource).toBe(resource.nomRessource);
    expect(event.nomEditeur).toBe(resource.nomEditeur);
    expect(event.urlVignette).toBe(resource.urlVignette);
    expect(event.typePedagogique).toEqual(resource.typePedagogique);
    expect(event.niveauEducatif).toEqual(resource.niveauEducatif);
    expect(event.typePresentation).toEqual(resource.typePresentation);
    expect(event.typologieDocument).toEqual(resource.typologieDocument);
    expect(event.domaineEnseignement).toEqual(resource.domaineEnseignement);
  });
});

describe("toJSON()", () => {
  it("should match with the mocked event", () => {
    expect(event.toJSON()).toEqual(mockedEvent);
  });
});

describe("save(): Promise<any>", () => {
  it("should send data in body matching mocked event", async () => {
    (http.post as jest.Mock).mockResolvedValueOnce(mockHttpResponse(mockedEvent));
    const { data } = await event.save();

    expect(http.post).toHaveBeenCalledWith("/gar/event", mockedEvent);
    expect(data).toEqual(mockedEvent);
  });
});
