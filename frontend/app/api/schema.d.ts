/**
 * This file was auto-generated by openapi-typescript.
 * Do not make direct changes to the file.
 */

export interface paths {
    "/me/password": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        /**
         * [WIP] - Set a new password
         * @description Sets a new password for currently logged in user
         */
        put: {
            parameters: {
                query?: never;
                header?: never;
                path?: never;
                cookie?: never;
            };
            requestBody: {
                content: {
                    "application/json": {
                        /** @description The new password */
                        password?: string;
                    };
                };
            };
            responses: {
                /** @description New password set successfully */
                200: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content?: never;
                };
                /** @description Unauthorized */
                401: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content?: never;
                };
                /** @description Internal Server Error */
                500: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content?: never;
                };
            };
        };
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/members": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * List all club members
         * @description Returns a list of all club members
         */
        get: {
            parameters: {
                query?: {
                    /** @description Defines set of returned data
                     *     - full: all member data what are displayable to user are returned
                     *     - compact: `id`, `firstName`, `lastName`, 'registrationNumber`
                     *      */
                    view?: "full" | "compact";
                    /** @description | value | effect |
                     *     | --- | --- |
                     *     | `true` | returns both active and suspended members |
                     *     | `false` | return only active members |
                     *      */
                    suspended?: boolean;
                };
                header?: never;
                path?: never;
                cookie?: never;
            };
            requestBody?: never;
            responses: {
                /** @description A list of club members */
                200: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content: {
                        "application/json": components["schemas"]["MembersList"];
                    };
                };
                401: components["responses"]["401"];
            };
        };
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/members/{memberId}": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description ID of member */
                memberId: components["parameters"]["MemberIdPath"];
            };
            cookie?: never;
        };
        /**
         * Get member by ID
         * @description Returns a member
         */
        get: {
            parameters: {
                query?: never;
                header?: never;
                path: {
                    /** @description ID of member */
                    memberId: components["parameters"]["MemberIdPath"];
                };
                cookie?: never;
            };
            requestBody?: never;
            responses: {
                /** @description A single member */
                200: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content: {
                        "application/json": components["schemas"]["Member"];
                    };
                };
                401: components["responses"]["401"];
                404: components["responses"]["404"];
            };
        };
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/members/{memberId}/editMemberInfoForm": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description ID of member */
                memberId: components["parameters"]["MemberIdPath"];
            };
            cookie?: never;
        };
        /** Returns data for edit member information form */
        get: {
            parameters: {
                query?: never;
                header?: never;
                path: {
                    /** @description ID of member */
                    memberId: components["parameters"]["MemberIdPath"];
                };
                cookie?: never;
            };
            requestBody?: never;
            responses: {
                /** @description Club member updated successfully */
                200: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content: {
                        "application/json": components["schemas"]["MemberEditForm"];
                    };
                };
                400: components["responses"]["400"];
                401: components["responses"]["401"];
                /** @description Forbidden - User does not have permission to update a member */
                403: components["responses"]["403"];
                /** @description Club member not found */
                404: components["responses"]["401"];
            };
        };
        /** Update member information */
        put: {
            parameters: {
                query?: never;
                header?: never;
                path: {
                    /** @description ID of member */
                    memberId: components["parameters"]["MemberIdPath"];
                };
                cookie?: never;
            };
            requestBody: {
                content: {
                    "application/json": components["schemas"]["MemberEditForm"];
                };
            };
            responses: {
                /** @description Club member updated successfully */
                200: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content?: never;
                };
                400: components["responses"]["400"];
                401: components["responses"]["401"];
                /** @description Forbidden - User does not have permission to update a member */
                403: components["responses"]["403"];
                /** @description Club member not found */
                404: components["responses"]["401"];
            };
        };
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/members/{memberId}/suspendMembershipForm": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description ID of member */
                memberId: components["parameters"]["MemberIdPath"];
            };
            cookie?: never;
        };
        /**
         * Retrieve information about member account status for membership suspension
         * @description Returns information about member account to be suspended.
         *
         *     #### Required authorization
         *     requires `members:suspendMembership` grant
         *
         */
        get: {
            parameters: {
                query?: never;
                header?: never;
                path: {
                    /** @description ID of member */
                    memberId: components["parameters"]["MemberIdPath"];
                };
                cookie?: never;
            };
            requestBody?: never;
            responses: {
                /** @description details about member account important for membership suspension */
                200: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content: {
                        "application/json": components["schemas"]["MembershipSuspensionInfo"];
                    };
                };
                403: components["responses"]["403"];
                /** @description Club member not found */
                404: components["responses"]["401"];
            };
        };
        put?: never;
        /**
         * Suspend membership for a club member
         * @description Suspends membership for a club member.
         *
         *     If there are some blockers (debt, etc), it responds with HTTP '409' unless `force=true` parameter was used.
         *
         *     #### Required authorization
         *     requires `members:suspendMembership` grant
         *
         */
        post: {
            parameters: {
                query?: {
                    /** @description Forces membership suspension for member even if there are some reasons (like negative finance account balance, etc..) why it would be wise to postpone user membership suspension */
                    force?: boolean;
                };
                header?: never;
                path: {
                    /** @description ID of member */
                    memberId: components["parameters"]["MemberIdPath"];
                };
                cookie?: never;
            };
            requestBody?: never;
            responses: {
                /** @description Membership of club member was suspended successfully */
                200: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content?: never;
                };
                400: components["responses"]["400"];
                401: components["responses"]["401"];
                /** @description Forbidden - User does not have permission to update a member */
                403: components["responses"]["403"];
                /** @description Club member not found */
                404: components["responses"]["401"];
                /** @description It's not possible to suspend membership for club member. See response body for actual reason(s). You may use `force` to override these reasons. */
                409: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content: {
                        "application/problem+json": components["schemas"]["RFC7807ErrorResponse"] & {
                            blockers: components["schemas"]["SuspendMembershipBlockers"];
                        };
                    };
                };
            };
        };
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/registrationNumber": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get recommended registration number for sex and date of birth
         * @description #### Required authorization
         *     requires `members:register` grant
         *
         */
        get: {
            parameters: {
                query: {
                    dateOfBirth: string;
                    sex: components["schemas"]["Sex"];
                };
                header?: never;
                path?: never;
                cookie?: never;
            };
            requestBody?: never;
            responses: {
                /** @description Recommended (available) registration number for new member registration */
                200: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content: {
                        "application/json": {
                            suggestedRegistrationNumber: components["schemas"]["RegistrationNumber"];
                        };
                    };
                };
                400: components["responses"]["400"];
                401: components["responses"]["401"];
            };
        };
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/oris/userInfo/{regNum}": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /**
         * Get information about user from ORIS
         * @description #### Required authorization
         *     requires `members:register` grant
         *
         */
        get: {
            parameters: {
                query?: never;
                header?: never;
                path: {
                    /** @description Registration number of user to retrieve ORIS data about */
                    regNum: components["schemas"]["RegistrationNumber"];
                };
                cookie?: never;
            };
            requestBody?: never;
            responses: {
                /** @description Available information about user read from ORIS */
                200: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content: {
                        "application/json": components["schemas"]["ORISUserInfo"];
                    };
                };
                400: components["responses"]["400"];
                401: components["responses"]["401"];
                403: components["responses"]["403"];
                404: components["responses"]["401"];
            };
        };
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/memberRegistrations": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        get?: never;
        put?: never;
        /**
         * Register a new club member
         * @description Registers a new club member with the provided details.
         *
         *     #### Required authorization
         *     requires `members:register` grant
         *
         */
        post: {
            parameters: {
                query?: never;
                header?: never;
                path?: never;
                cookie?: never;
            };
            requestBody: {
                content: {
                    "application/json": components["schemas"]["MemberRegistrationForm"];
                };
            };
            responses: {
                /** @description Registration was processed successfully */
                201: {
                    headers: {
                        /** @description URI to endpoint where details about created member can be retrieved */
                        Location: string;
                        /** @description ID of created member */
                        MemberId: components["schemas"]["MemberId"];
                        [name: string]: unknown;
                    };
                    content?: never;
                };
                400: components["responses"]["400"];
                401: components["responses"]["401"];
                /** @description Forbidden - User does not have permission for this action */
                403: components["responses"]["403"];
                409: components["responses"]["409MemberRegistration"];
            };
        };
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/cus/exports/members": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** [WIP] - export users in CUS format */
        get: {
            parameters: {
                query?: never;
                header?: never;
                path?: never;
                cookie?: never;
            };
            requestBody?: never;
            responses: {
                /** @description A list of differences */
                200: {
                    headers: {
                        [name: string]: unknown;
                    };
                    content?: never;
                };
                401: components["responses"]["401"];
                403: components["responses"]["403"];
            };
        };
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/grants": {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        /** returns details about available security grants what can be assigned to users */
        get: operations["getAllGrants"];
        put?: never;
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
    "/members/{memberId}/changeGrantsForm": {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description ID of member */
                memberId: components["parameters"]["MemberIdPath"];
            };
            cookie?: never;
        };
        /**
         * returns grants assigned to member
         * @description Requires `members:permissions` grant
         */
        get: operations["getMemberGrants"];
        /**
         * updates grants assigned to member
         * @description Requires `members:permissions` grant
         */
        put: operations["updateMemberGrants"];
        post?: never;
        delete?: never;
        options?: never;
        head?: never;
        patch?: never;
        trace?: never;
    };
}
export type webhooks = Record<string, never>;
export interface components {
    schemas: {
        /** @description two letter country code, ISO 3166-1 alpha-2 */
        CountryCode: string;
        /** @description Birth certificate number for Czech citizens */
        BirthCertificateNumber: string;
        /** @description ORIS registration number */
        RegistrationNumber: string;
        /** @description Oris ID of registered orienteering runner */
        OrisID: number;
        /** @enum {string} */
        DrivingLicence: "B" | "BE" | "C" | "D";
        /** @description SI chip used by member */
        SICard: number;
        /** @enum {string} */
        Sex: "male" | "female";
        /** @description At least one of email or phone value is required */
        Contact: {
            /**
             * Format: email
             * @description Email address of the club member or guardian
             */
            email: string;
            /** @description Phone number of the club member or guardian */
            phone: string;
            /** @description Note about the contact */
            note?: string;
        };
        Address: {
            /** @description Street name and number */
            streetAndNumber: string;
            /** @description City */
            city: string;
            /** @description Postal or ZIP code */
            postalCode: string;
            country: components["schemas"]["CountryCode"];
        };
        IdentityCard: {
            /** @description Personal identification number of the club member */
            number?: string;
            /**
             * Format: date
             * @description Expiry date of the ID card, YYYY-MM-DD
             */
            expiryDate?: string;
        };
        /** @description 'compact' view of Member
         *      */
        MemberViewCompact: {
            readonly id: components["schemas"]["MemberId"];
            /** @description First name of the club member */
            firstName: string;
            /** @description Last name of the club member */
            lastName: string;
            registrationNumber: components["schemas"]["RegistrationNumber"];
        };
        /** @description Unique identifier for the club member */
        MemberId: number;
        Member: components["schemas"]["MemberViewCompact"] & {
            id: components["schemas"]["MemberId"];
            birthCertificateNumber?: components["schemas"]["BirthCertificateNumber"];
            identityCard?: components["schemas"]["IdentityCard"];
            address: components["schemas"]["Address"];
            /**
             * Format: date
             * @description Date of birth of the club member
             */
            dateOfBirth: string;
            contact?: components["schemas"]["Contact"];
            legalGuardians?: components["schemas"]["LegalGuardian"][];
            /** @description Chip number assigned to the club member */
            siCard?: number;
            nationality: components["schemas"]["CountryCode"];
            sex: components["schemas"]["Sex"];
            licences?: components["schemas"]["Licences"];
            bankAccount?: components["schemas"]["BankAccountNumber"];
            /** @description Dietary restrictions of the club member */
            dietaryRestrictions?: string;
            drivingLicence?: components["schemas"]["DrivingLicence"][];
            /** @description Whether the club member has completed the medic course */
            medicCourse?: boolean;
        } & unknown;
        /** @description List of members. */
        MembersList: {
            items: (components["schemas"]["Member"] | components["schemas"]["MemberViewCompact"])[];
        };
        /** @description User data retrieved from ORIS
         *
         *     #### Required authorization
         *     - requires `members:register` grant */
        ORISUserInfo: {
            /** @description First name of the club member */
            firstName: string;
            /** @description Last name of the club member */
            lastName: string;
            registrationNumber: components["schemas"]["RegistrationNumber"];
            orisId?: components["schemas"]["OrisID"];
        };
        /** @description Data required to register new member.
         *
         *     #### Required authorization
         *     - requires `members:register` grant
         *
         *     Additional validations:
         *     - either contact or guardian needs to be set
         *     - when nationality is different than `CZ`, `birthCertificateNumber` value will be ignored */
        MemberRegistrationForm: {
            /** @description First name of the club member */
            firstName: string;
            /** @description Last name of the club member */
            lastName: string;
            sex: components["schemas"]["Sex"];
            /**
             * Format: date
             * @description Date of birth of the club member
             */
            dateOfBirth: string;
            birthCertificateNumber?: components["schemas"]["BirthCertificateNumber"];
            nationality: components["schemas"]["CountryCode"];
            address: components["schemas"]["Address"];
            contact?: components["schemas"]["Contact"];
            guardians?: components["schemas"]["LegalGuardian"][];
            siCard?: components["schemas"]["SICard"];
            bankAccount?: components["schemas"]["BankAccountNumber"];
            registrationNumber?: components["schemas"]["RegistrationNumber"];
            orisId?: components["schemas"]["OrisID"];
        };
        /** @description Member attributes editable by authorized user who can change details about other members
         *
         *     #### Required authorization
         *     - requires `members:edit` grant
         *
         *     Additional validations:
         *     - when `CZ` is selected as nationality, then `birthCertificateNumber` is required value */
        EditAnotherMemberDetailsForm: {
            /** @description First name of the club member */
            firstName: string;
            /** @description Last name of the club member */
            lastName: string;
            /**
             * Format: date
             * @description Date of birth of the club member
             */
            dateOfBirth: string;
            birthCertificateNumber?: components["schemas"]["BirthCertificateNumber"];
            nationality: components["schemas"]["CountryCode"];
            sex: components["schemas"]["Sex"];
        };
        /** @description Member attributes which can be updated by member himself (member can update some own attributes)
         *
         *     #### Required authorization
         *     - user can edit own member data
         *
         *     Additional validations:
         *     - either contact or at least 1 guardian needs to be entered
         *      */
        EditMyDetailsForm: {
            identityCard?: components["schemas"]["IdentityCard"];
            nationality: components["schemas"]["CountryCode"];
            address: components["schemas"]["Address"];
            contact?: components["schemas"]["Contact"];
            guardians?: components["schemas"]["LegalGuardian"][];
            siCard?: components["schemas"]["SICard"];
            bankAccount?: components["schemas"]["BankAccountNumber"];
            /** @description Dietary restrictions of the club member */
            dietaryRestrictions?: string;
            drivingLicence?: components["schemas"]["DrivingLicence"][];
            /** @description Whether the club member has completed the medic course */
            medicCourse?: boolean;
        };
        LegalGuardian: {
            /** @description First name of the guardian */
            firstName: string;
            /** @description Last name of the guardian */
            lastName: string;
            contact: components["schemas"]["Contact"];
            /** @description Note about the guardian (matka, otec) */
            note?: string;
        };
        /** @description Bank account number of the club member IBAN */
        BankAccountNumber: string;
        OBLicence: {
            /**
             * @description License number of the club member
             * @enum {string}
             */
            licence: "E" | "R" | "A" | "B" | "C";
        };
        RefereeLicence: {
            /**
             * @description referee license number of the club member
             * @enum {string}
             */
            licence: "R1" | "R2" | "R3";
            /**
             * Format: date
             * @description Expiry date of the license
             */
            expiryDate: string;
        };
        TrainerLicence: {
            /**
             * @description trainer license number of the club member
             * @enum {string}
             */
            licence: "T1" | "T2" | "T3";
            /**
             * Format: date
             * @description Expiry date of the license
             */
            expiryDate: string;
        };
        Licences: {
            ob?: components["schemas"]["OBLicence"];
            referee?: components["schemas"]["RefereeLicence"];
            trainer?: components["schemas"]["TrainerLicence"];
        };
        /** @description Form to edit member information */
        MemberEditForm: components["schemas"]["EditMyDetailsForm"] | components["schemas"]["EditAnotherMemberDetailsForm"];
        MembershipSuspensionInfo: {
            /** @description tells if member account is currently suspended */
            isSuspended: boolean;
            /** @description tells if member account can be suspended */
            canSuspend: boolean;
            details: components["schemas"]["SuspendMembershipBlockers"];
        };
        /** @description describes conditions which may prevent membership suspension and their actual status */
        SuspendMembershipBlockers: {
            finance: {
                /** @description tells if finance account balance permits membership suspension */
                status: boolean;
            };
        };
        /**
         * @description Global grants are assigned to users and are valid globally in the application.
         *
         *     | Grant name | granted permissions |
         *     | --- | --- |
         *     | `members:register` | can create new members |
         *     | `members:edit` | can edit selected attributes for all existing members |
         *     | `members:suspendMembership` | can suspend membership for club members |
         *
         * @enum {string}
         */
        GlobalGrants: "members:register" | "members:edit" | "members:suspendMembership" | "members:permissions";
        /**
         * @description Member specific grants are defined between 2 users (user is allowed to perform specific action on behalf of another user). These define fine-grained permissions and can be granted explicitely to selected users or through permissions granted from membership between members of user groups.
         *
         *     | Grant name | granted permissions |
         *     | --- | --- |
         *     | `members#canDisplayMemberPersonalContact` | can display personal contact information of member |
         *     | `members#canDisplayMemberLegalGuardianContact` | can display contact information of legal guardian of member |
         *     | `members#canDisplayMemberAddress` | can display contact information of legal guardian of member |
         *
         * @enum {string}
         */
        MemberSpecificGrants: "members#canDisplayMemberPersonalContact" | "members#canDisplayMemberLegalGuardianContact" | "members#canDisplayMemberAddress";
        GlobalGrantDetail: {
            grant?: components["schemas"]["GlobalGrants"];
            /** @description User friendly description of the grant */
            description?: string;
        };
        /** @description Data for form setting member grants */
        MemberGrantsForm: {
            grants?: components["schemas"]["GlobalGrants"][];
        };
        RFC7807ErrorResponse: {
            /** @description Description of the error status */
            title: string;
            /** @description error status value */
            status: number;
            /** @description User friendly description of the error */
            detail: string;
            /** @description URI of the resource which has thrown the error */
            instance: string;
            type?: string;
        };
    };
    responses: {
        /** @description Invalid user input */
        400: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["RFC7807ErrorResponse"] & {
                    validationErrors?: {
                        fieldName?: string;
                        errorMessage?: string;
                    }[];
                };
            };
        };
        /** @description Missing required user authentication or authentication failed */
        401: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["RFC7807ErrorResponse"];
            };
        };
        /** @description User is not allowed to perform requested operation */
        403: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["RFC7807ErrorResponse"] & {
                    missingGrant?: components["schemas"]["GlobalGrants"] | components["schemas"]["MemberSpecificGrants"];
                };
            };
        };
        /** @description Requested resource wasn't found */
        404: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["RFC7807ErrorResponse"];
            };
        };
        /** @description Operation can't be done because of conflicting resource */
        409: {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["RFC7807ErrorResponse"];
            };
        };
        /** @description Conflict - Member already exists (usually registration was submitted with existing registration number) */
        "409MemberRegistration": {
            headers: {
                [name: string]: unknown;
            };
            content: {
                "application/problem+json": components["schemas"]["RFC7807ErrorResponse"] & {
                    /**
                     * Format: uuid
                     * @description ID of conflicting member
                     */
                    existingUserId?: string;
                };
            };
        };
    };
    parameters: {
        /** @description ID of member */
        MemberIdPath: components["schemas"]["MemberId"];
    };
    requestBodies: never;
    headers: never;
    pathItems: never;
}
export type $defs = Record<string, never>;
export interface operations {
    getAllGrants: {
        parameters: {
            query?: never;
            header?: never;
            path?: never;
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description List of grants which can be assigned to members */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": {
                        grants?: components["schemas"]["GlobalGrantDetail"][];
                    };
                };
            };
        };
    };
    getMemberGrants: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description ID of member */
                memberId: components["parameters"]["MemberIdPath"];
            };
            cookie?: never;
        };
        requestBody?: never;
        responses: {
            /** @description Edit member grants form content */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content: {
                    "application/json": components["schemas"]["MemberGrantsForm"];
                };
            };
        };
    };
    updateMemberGrants: {
        parameters: {
            query?: never;
            header?: never;
            path: {
                /** @description ID of member */
                memberId: components["parameters"]["MemberIdPath"];
            };
            cookie?: never;
        };
        requestBody?: {
            content: {
                "application/json": components["schemas"]["MemberGrantsForm"];
            };
        };
        responses: {
            /** @description Member grants were successfully updated */
            200: {
                headers: {
                    [name: string]: unknown;
                };
                content?: never;
            };
        };
    };
}