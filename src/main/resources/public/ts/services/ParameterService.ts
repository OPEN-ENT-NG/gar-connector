import {ng} from 'entcore'
import http from 'axios';

export interface StructureGar {
   uai: string;
   name: string;
   structureId: string;
   id: string;
}

export interface ParameterService {
    export(): void;
    getStructureGar(): Promise<StructureGar>;
    createGroupGarToStructure(name: string, structureId: string): Promise<void>;
    addUsersToGarGroup(groupId: string, structureId: string): Promise<void>;
}

export const ParameterService = ng.service('ParameterService', (): ParameterService => ({
    export: () => {
        const url = '/mediacentre/launchExport';
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
            const {data} = await http.post(`structure/gar/group`, {name: name, structureId: structureId});
            return data;
        } catch (err) {
            throw err;
        }
    },

    addUsersToGarGroup: async (groupId: string, structureId: string) => {
        try {
            const {data} = await http.post(`structure/gar/group/user`, {groupId: groupId, structureId: structureId});
            return data;
        } catch (err) {
            throw err;
        }
    }
}));
