import {appPrefix, ng, template} from "entcore";
import {ParameterService} from "../services";
import {Utils} from "../utils/Utils";

declare const window: any;

/**
 Parameter controller
 ------------------.
 **/
export const parameterController = ng.controller("ParameterController", [
    "$scope", "ParameterService", async ($scope, ParameterService: ParameterService) => {
        await template.open("main", "parameter");
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
        ParameterService.getStructureGar().then(async (structures) => {
            $scope.structureGarLists = structures;
            $scope.structureGarLists.map((structure) => structure.number_deployed = structure.deployed ? 1 : 0);
            await Utils.safeApply($scope);
        });
        /* button handler */
        $scope.createButton = false;
        $scope.$watch(() => $scope.structureGarLists, getDeployedCounter);

        $scope.match = function () {
            return function (item) {
                if ($scope.filter.value.trim() === '') return true;
                return item.name.toLowerCase().includes($scope.filter.value.toLowerCase())
                    || item.uai.toLowerCase().includes($scope.filter.value.toLowerCase());
            }
        };

        $scope.export = () => {
            ParameterService.export();
        };

        function getDeployedCounter(): void {
            let counter = 0;
            $scope.structureGarLists.map(({deployed}) => counter += deployed);
            $scope.counter.value = counter;
        }

        $scope.createGarGroup = async ({structureId, deployed}) => {
            let response;
            $scope.createButton = true;
            await Utils.safeApply($scope);
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
            await Utils.safeApply($scope);
        };

        $scope.showRespAffecGarGroup = function ({structureId, id}) {
            window.open(`/admin/${structureId}/groups/manual/${id}/details`);
        };

        $scope.addUser = async (groupId, structureId, source) => {
            $scope.createButton = true;
            await Utils.safeApply($scope);
            await ParameterService.addUsersToGarGroup(groupId, structureId, source);
            $scope.createButton = false;
            await Utils.safeApply($scope);
        };

        $scope.testMail = () => window.open(`/${appPrefix}/mail/test`);
        $scope.downloadArchive = () => window.open(`/${appPrefix}/export/archive`);
        $scope.downloadXSDValidation = () => window.open(`/${appPrefix}/export/xsd/validation`);
    }]);
