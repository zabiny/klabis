export const COMPOSITE_SUBFIELDS: Record<string, string[]> = {
    AddressRequest: ['street', 'city', 'postalCode', 'country'],
    IdentityCardRequest: ['cardNumber', 'validityDate'],
    GuardianRequest: ['firstName', 'lastName', 'relationship', 'email', 'phone'],
    MedicalCourseRequest: ['completionDate', 'validityDate'],
    TrainerLicenseRequest: ['licenseNumber', 'validityDate'],
};

export const COMPOSITE_DATE_SUBFIELDS = new Set(['validityDate', 'completionDate']);
