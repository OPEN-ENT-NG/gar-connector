import {appPrefix, ng, template} from "entcore";
import {ParameterService} from "../services";

declare const window: any;

/**
 Parameter controller
 ------------------.
 **/
export const parameterController = ng.controller("ParameterController", [
    "$scope", "ParameterService", async ($scope, ParameterService: ParameterService) => {
        template.open("main", "parameter");
        $scope.counter = {
            value: 0
        };

        $scope.filter = {
            property: 'uai',
            desc: false,
            value: ''
        };

        const GROUP_GAR_NAME = "RESP-AFFECT-GAR";
        $scope.structureGarLists = [];
        ParameterService.getStructureGar().then(structures => {
            $scope.structureGarLists = structures;
            $scope.structureGarLists.map((structure) => structure.number_deployed = structure.deployed ? 1 : 0);
            $scope.$apply();
        });

        $scope.match = function () {
            return function (item) {
                if ($scope.filter.value.trim() === '') return true;
                return item.name.toLowerCase().includes($scope.filter.value.toLowerCase())
                    || item.uai.toLowerCase().includes($scope.filter.value.toLowerCase());
            }
        };

        /* button handler */
        $scope.createButton = false;
        $scope.addButton = false;
        $scope.$apply();

        $scope.export = () => {
            ParameterService.export();
        };

        function getDeployedCounter(): void {
            let counter = 0;
            $scope.structureGarLists.map(({deployed}) => counter += deployed);
            $scope.counter.value = counter;
        }

        $scope.$watch(() => $scope.structureGarLists, getDeployedCounter);

        $scope.createGarGroup = async ({structureId, deployed}) => {
            let response;
            $scope.createButton = true;
            $scope.$apply();
            if (!deployed) {
                response = await ParameterService.createGroupGarToStructure(GROUP_GAR_NAME, structureId);
            } else {
                response = await ParameterService.undeployStructure(structureId);
            }
            if (response.status === 200) {
                $scope.structureGarLists = await ParameterService.getStructureGar();
                $scope.structureGarLists.map((structure) => structure.number_deployed = structure.deployed ? 1 : 0);
            }
            $scope.createButton = false;
            $scope.$apply();
        };

        $scope.showRespAffecGarGroup = function ({structureId, id}) {
            window.open(`/admin/${structureId}/groups/manual/${id}/details`);
        };

        $scope.addUser = async (groupId, structureId, source) => {
            $scope.addButton = true;
            $scope.$apply();
            await ParameterService.addUsersToGarGroup(groupId, structureId, source);
            $scope.addButton = false;
            $scope.$apply();
        };

        $scope.testMail = () => window.open(`/${appPrefix}/mail/test`);
        $scope.downloadArchive = () => window.open(`/${appPrefix}/export/archive`);
        $scope.downloadXSDValidation = () => window.open(`/${appPrefix}/export/xsd/validation`);
    }]);
