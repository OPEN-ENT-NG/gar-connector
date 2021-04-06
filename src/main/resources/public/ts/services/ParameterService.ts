import {ng} from 'entcore'
import http from 'axios';

export interface StructureGar {
   uai: string;
   name: string;
   structureId: string;
   id: string;
   source: string;
}

export interface ParameterService {
    export(): void;
    getStructureGar(): Promise<StructureGar>;
    createGroupGarToStructure(name: string, structureId: string): Promise<any>;
    addUsersToGarGroup(groupId: string, structureId: string, source: string): Promise<void>;

    undeployStructure(id: string);
}

export const ParameterService = ng.service('ParameterService', (): ParameterService => ({
    export: () => {
        const url = '/gar/launchExport';
        window.open(url);
    },

    getStructureGar: async () => {
        try {
            const {data} = await http.get(`structure/gar`);
            return data;
        } catch (err) {
            throw err;
        }
    },

    createGroupGarToStructure: async (name: string, structureId: string) => {
        try {
            return await http.post(`structure/gar/group`, {name: name, structureId: structureId});
        } catch (err) {
            throw err;
        }
    },

    addUsersToGarGroup: async (groupId: string, structureId: string, source: string) => {
        try {
            const {data} = await http.post(`structure/gar/group/user`, {groupId: groupId, structureId: structureId, source: source});
            return data;
        } catch (err) {
            throw err;
        }
    },

    undeployStructure: async (id: string) => await http.delete(`/gar/structures/${id}`)
}));

