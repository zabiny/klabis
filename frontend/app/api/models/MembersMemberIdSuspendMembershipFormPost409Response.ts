/* tslint:disable */
/* eslint-disable */
/**
 * Klabis API
 * --- # Work In progress Application is currently under development: - changes may be done to any part of API - saved data may be reset at any moment (currently at any API server restart) --- # Glossary - `member` - club member who can use the application    - `user` - logged in member - `grant` - configurable permission allowing user to perform selected action or view some data  # Authorization Every operation changing data (and some view requests) require `grant` which represents permission for the user to perform such operation.     There are 2 types of grants:   ## Global grants These grants are assigned to user and are valid globally in the application. They grant permission for operations like Create new member, etc.   ## Member specific grants These grants represents permission to perform operation (or view data) on behalf of selected user. User can receive this grant in two ways: - user may allow another member to perform operation on his behalf/view his data - user is granted permission to perform operation on behalf of another member / view another member\'s data because of membership/leadership of user group  Operations / data protected by this type of grant can be automatically performed if user is same person as member who is described by protected data or if operation is changing data of such member.     ## What authorization is required to use API endpoint? If endpoint requires authorization, it is written in description text of such endpoint with label \"Required authorization\"  ## What authorization is required to see value of attribute in the response? Response attributes: even some attributes in the response may require specific grant - see description of the attribute in response OpenAPI/JSON schema. If user doesn\'t hold such grant, attribute will be returned as empty (null)    # API versioning To be added later (before first production release). At current stage it\'s not needed. Most likely either contentType or request header versioning strategy will be used.   
 *
 * The version of the OpenAPI document: 0.2.1
 * Contact: klabis@otakar.io
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */

import { mapValues } from '../runtime';
import type { SuspendMembershipBlockers } from './SuspendMembershipBlockers';
import {
    SuspendMembershipBlockersFromJSON,
    SuspendMembershipBlockersFromJSONTyped,
    SuspendMembershipBlockersToJSON,
} from './SuspendMembershipBlockers';

/**
 * 
 * @export
 * @interface MembersMemberIdSuspendMembershipFormPost409Response
 */
export interface MembersMemberIdSuspendMembershipFormPost409Response {
    /**
     * Description of the error status
     * @type {string}
     * @memberof MembersMemberIdSuspendMembershipFormPost409Response
     */
    title: string;
    /**
     * error status value
     * @type {number}
     * @memberof MembersMemberIdSuspendMembershipFormPost409Response
     */
    status: number;
    /**
     * User friendly description of the error
     * @type {string}
     * @memberof MembersMemberIdSuspendMembershipFormPost409Response
     */
    detail: string;
    /**
     * URI of the resource which has thrown the error
     * @type {string}
     * @memberof MembersMemberIdSuspendMembershipFormPost409Response
     */
    instance: string;
    /**
     * 
     * @type {string}
     * @memberof MembersMemberIdSuspendMembershipFormPost409Response
     */
    type?: string;
    /**
     * 
     * @type {SuspendMembershipBlockers}
     * @memberof MembersMemberIdSuspendMembershipFormPost409Response
     */
    blockers: SuspendMembershipBlockers;
}

/**
 * Check if a given object implements the MembersMemberIdSuspendMembershipFormPost409Response interface.
 */
export function instanceOfMembersMemberIdSuspendMembershipFormPost409Response(value: object): value is MembersMemberIdSuspendMembershipFormPost409Response {
    if (!('title' in value) || value['title'] === undefined) return false;
    if (!('status' in value) || value['status'] === undefined) return false;
    if (!('detail' in value) || value['detail'] === undefined) return false;
    if (!('instance' in value) || value['instance'] === undefined) return false;
    if (!('blockers' in value) || value['blockers'] === undefined) return false;
    return true;
}

export function MembersMemberIdSuspendMembershipFormPost409ResponseFromJSON(json: any): MembersMemberIdSuspendMembershipFormPost409Response {
    return MembersMemberIdSuspendMembershipFormPost409ResponseFromJSONTyped(json, false);
}

export function MembersMemberIdSuspendMembershipFormPost409ResponseFromJSONTyped(json: any, ignoreDiscriminator: boolean): MembersMemberIdSuspendMembershipFormPost409Response {
    if (json == null) {
        return json;
    }
    return {
        
        'title': json['title'],
        'status': json['status'],
        'detail': json['detail'],
        'instance': json['instance'],
        'type': json['type'] == null ? undefined : json['type'],
        'blockers': SuspendMembershipBlockersFromJSON(json['blockers']),
    };
}

export function MembersMemberIdSuspendMembershipFormPost409ResponseToJSON(value?: MembersMemberIdSuspendMembershipFormPost409Response | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'title': value['title'],
        'status': value['status'],
        'detail': value['detail'],
        'instance': value['instance'],
        'type': value['type'],
        'blockers': SuspendMembershipBlockersToJSON(value['blockers']),
    };
}
