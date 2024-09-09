/* tslint:disable */
/* eslint-disable */
import {Configuration} from "@/api/runtime";
import {MembersApi, ORISApi} from "@/api/apis";

export * from './runtime';
export * from './apis/index';
export * from './models/index';


const configuration = new Configuration();
export const membersApi = new MembersApi(configuration);
export const orisApi = new ORISApi(configuration);
