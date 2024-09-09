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
import type { LegalGuardian } from './LegalGuardian';
import {
    LegalGuardianFromJSON,
    LegalGuardianFromJSONTyped,
    LegalGuardianToJSON,
} from './LegalGuardian';
import type { Address } from './Address';
import {
    AddressFromJSON,
    AddressFromJSONTyped,
    AddressToJSON,
} from './Address';
import type { DrivingLicence } from './DrivingLicence';
import {
    DrivingLicenceFromJSON,
    DrivingLicenceFromJSONTyped,
    DrivingLicenceToJSON,
} from './DrivingLicence';
import type { Contact } from './Contact';
import {
    ContactFromJSON,
    ContactFromJSONTyped,
    ContactToJSON,
} from './Contact';
import type { IdentityCard } from './IdentityCard';
import {
    IdentityCardFromJSON,
    IdentityCardFromJSONTyped,
    IdentityCardToJSON,
} from './IdentityCard';

/**
 * Member attributes which can be updated by member himself (member can update some own attributes)  
 * 
 * #### Required authorization
 * - user can edit own member data 
 * 
 * Additional validations:
 * - either contact or at least 1 guardian needs to be entered
 * 
 * @export
 * @interface EditMyDetailsForm
 */
export interface EditMyDetailsForm {
    /**
     * 
     * @type {IdentityCard}
     * @memberof EditMyDetailsForm
     */
    identityCard?: IdentityCard;
    /**
     * two letter country code, ISO 3166-1 alpha-2
     * @type {string}
     * @memberof EditMyDetailsForm
     */
    nationality: string;
    /**
     * 
     * @type {Address}
     * @memberof EditMyDetailsForm
     */
    address: Address;
    /**
     * 
     * @type {Contact}
     * @memberof EditMyDetailsForm
     */
    contact?: Contact;
    /**
     * 
     * @type {Array<LegalGuardian>}
     * @memberof EditMyDetailsForm
     */
    guardians?: Array<LegalGuardian>;
    /**
     * SI chip used by member
     * @type {number}
     * @memberof EditMyDetailsForm
     */
    siCard?: number;
    /**
     * Bank account number of the club member IBAN
     * @type {string}
     * @memberof EditMyDetailsForm
     */
    bankAccount?: string;
    /**
     * Dietary restrictions of the club member
     * @type {string}
     * @memberof EditMyDetailsForm
     */
    dietaryRestrictions?: string;
    /**
     * 
     * @type {Array<DrivingLicence>}
     * @memberof EditMyDetailsForm
     */
    drivingLicence?: Array<DrivingLicence>;
    /**
     * Whether the club member has completed the medic course
     * @type {boolean}
     * @memberof EditMyDetailsForm
     */
    medicCourse?: boolean;
}

/**
 * Check if a given object implements the EditMyDetailsForm interface.
 */
export function instanceOfEditMyDetailsForm(value: object): value is EditMyDetailsForm {
    if (!('nationality' in value) || value['nationality'] === undefined) return false;
    if (!('address' in value) || value['address'] === undefined) return false;
    return true;
}

export function EditMyDetailsFormFromJSON(json: any): EditMyDetailsForm {
    return EditMyDetailsFormFromJSONTyped(json, false);
}

export function EditMyDetailsFormFromJSONTyped(json: any, ignoreDiscriminator: boolean): EditMyDetailsForm {
    if (json == null) {
        return json;
    }
    return {
        
        'identityCard': json['identityCard'] == null ? undefined : IdentityCardFromJSON(json['identityCard']),
        'nationality': json['nationality'],
        'address': AddressFromJSON(json['address']),
        'contact': json['contact'] == null ? undefined : ContactFromJSON(json['contact']),
        'guardians': json['guardians'] == null ? undefined : ((json['guardians'] as Array<any>).map(LegalGuardianFromJSON)),
        'siCard': json['siCard'] == null ? undefined : json['siCard'],
        'bankAccount': json['bankAccount'] == null ? undefined : json['bankAccount'],
        'dietaryRestrictions': json['dietaryRestrictions'] == null ? undefined : json['dietaryRestrictions'],
        'drivingLicence': json['drivingLicence'] == null ? undefined : ((json['drivingLicence'] as Array<any>).map(DrivingLicenceFromJSON)),
        'medicCourse': json['medicCourse'] == null ? undefined : json['medicCourse'],
    };
}

export function EditMyDetailsFormToJSON(value?: EditMyDetailsForm | null): any {
    if (value == null) {
        return value;
    }
    return {
        
        'identityCard': IdentityCardToJSON(value['identityCard']),
        'nationality': value['nationality'],
        'address': AddressToJSON(value['address']),
        'contact': ContactToJSON(value['contact']),
        'guardians': value['guardians'] == null ? undefined : ((value['guardians'] as Array<any>).map(LegalGuardianToJSON)),
        'siCard': value['siCard'],
        'bankAccount': value['bankAccount'],
        'dietaryRestrictions': value['dietaryRestrictions'],
        'drivingLicence': value['drivingLicence'] == null ? undefined : ((value['drivingLicence'] as Array<any>).map(DrivingLicenceToJSON)),
        'medicCourse': value['medicCourse'],
    };
}
