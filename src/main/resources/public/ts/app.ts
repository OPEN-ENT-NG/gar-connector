import { ng, routes } from "entcore";
import { http, HttpRequestConfig } from 'entcore-toolkit';
import data from './model/__mocks__/ressources';
import * as controllers from "./controllers";

declare const demo;

for (let controller in controllers) {
  ng.controllers.push(controllers[controller]);
}

routes.define(function($routeProvider) {
  $routeProvider.otherwise({
    action: "defaultView"
  });
});

if (demo) {
    http.get = async function(url: string, config?: HttpRequestConfig) {
        return {
            status: 200,
            statusText: 'OK',
            headers: {},
            config,
            data: data.listeRessources.ressource
        }
    }
}