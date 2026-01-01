import {type ReactElement, useContext, useEffect, useRef, useState} from "react";
import type {EntityModel} from "../api";
import {TableCell} from "../components/KlabisTable";
import {HalLinksSection} from "../components/HalNavigator2/HalLinksSection.tsx";
import {HalEmbeddedTable} from "../components/HalNavigator2/HalEmbeddedTable.tsx";
import {HalFormsFormContext} from "../components/HalNavigator2/halforms";
import {useHalPageData} from "../hooks/useHalPageData.ts";
import {HalFormButton} from "../components/HalNavigator2/HalFormButton.tsx";
import {useFormikContext} from "formik";
import {Button} from "../components/UI";

type MemberListData = EntityModel<{
    id: number,
    firstName: string,
    lastName: string,
    registrationNumber: string
}>;

export const MembersPage = (): ReactElement => {
    const {route} = useHalPageData();

    return <div className="flex flex-col gap-8">
        <h1 className="text-3xl font-bold text-text-primary">Adresář</h1>

        <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
                <h2 className="text-xl font-bold text-text-primary">Členové</h2>
                <RegisterMemberFormButton/>
            </div>
            <HalEmbeddedTable<MemberListData> collectionName={"membersApiResponseList"} defaultOrderBy={"lastName"}
                                              onRowClick={route.navigateToResource}>
                <TableCell sortable column="firstName">Jméno</TableCell>
                <TableCell sortable column="lastName">Příjmení</TableCell>
                <TableCell sortable column="registrationNumber">Registrační číslo</TableCell>
                <TableCell column="_links"
                           dataRender={props => (
                               <HalLinksSection links={props.value as any}/>)}>Akce</TableCell>
            </HalEmbeddedTable>
        </div>
    </div>;

}

/**
 * Multi-step form component for member registration
 * Step 1: Personal info (firstName, lastName, sex, dateOfBirth, birthCertificateNumber, nationality)
 * Step 2: Contact info (address, contact, guardians)
 * Step 3: Member details (siCard, bankAccount, registrationNumber, orisId)
 */
const MultiStepMemberRegistrationForm = () => {
    const contextValue = useContext(HalFormsFormContext);
    const renderField = contextValue?.renderField || (() => null);
    const formik = useFormikContext();
    const [currentStep, setCurrentStep] = useState(1);
    const submissionAttemptedRef = useRef(false);

    // Monitor for submission errors and reset to step 1
    useEffect(() => {
        if (submissionAttemptedRef.current && !formik.isSubmitting) {
            // Check if there are any form errors (from either frontend or backend validation)
            const hasErrors = Object.keys(formik.errors).length > 0;
            if (hasErrors) {
                // Reset to step 1 so user can review values
                setCurrentStep(1);
                submissionAttemptedRef.current = false;
            } else {
                // Form was submitted successfully, modal will close
                submissionAttemptedRef.current = false;
            }
        }
    }, [formik.isSubmitting, formik.errors]);

    const validateStep = async (step: number): Promise<boolean> => {
        const fieldsToValidate = {
            1: ['firstName', 'lastName', 'sex', 'dateOfBirth', 'birthCertificateNumber', 'nationality'],
            2: ['address', 'contact'],
            3: ['siCard', 'bankAccount', 'registrationNumber', 'orisId'],
        };

        const fields = fieldsToValidate[step as keyof typeof fieldsToValidate] || [];

        // Mark all fields in this step as touched to show validation errors
        for (const field of fields) {
            await formik.setFieldTouched(field, true);
        }

        // Validate the entire form to populate errors
        const errors = await formik.validateForm();

        // Check if any of the fields for this step have errors
        const hasErrors = fields.some(field => errors[field]);
        return !hasErrors;
    };

    const handleNext = async () => {
        if (await validateStep(currentStep)) {
            setCurrentStep(currentStep + 1);
        }
    };

    const handleBack = () => {
        setCurrentStep(currentStep - 1);
    };

    const handleSubmit = async () => {
        if (await validateStep(currentStep)) {
            submissionAttemptedRef.current = true;
            // Trigger form submission
            formik.submitForm();
        }
    };

    return (
        <div className="space-y-6">
            {/* Step 1: Personal Info */}
            {currentStep === 1 && (
                <div className="space-y-4">
                    <h3 className="text-lg font-medium text-gray-700">Krok 1: Osobní údaje</h3>
                    <div className="space-y-4">
                        {renderField('firstName')}
                        {renderField('lastName')}
                        {renderField('sex')}
                        {renderField('dateOfBirth')}
                        {renderField('birthCertificateNumber')}
                        {renderField('nationality')}
                    </div>
                </div>
            )}

            {/* Step 2: Contact Info */}
            {currentStep === 2 && (
                <div className="space-y-4">
                    <h3 className="text-lg font-medium text-gray-700">Krok 2: Kontaktní informace</h3>
                    <div className="space-y-4">
                        {renderField('address')}
                        {renderField('contact')}
                        {renderField('guardians')}
                    </div>
                </div>
            )}

            {/* Step 3: Member Details */}
            {currentStep === 3 && (
                <div className="space-y-4">
                    <h3 className="text-lg font-medium text-gray-700">Krok 3: Údaje člena</h3>
                    <div className="space-y-4">
                        {renderField('siCard')}
                        {renderField('bankAccount')}
                        {renderField('registrationNumber')}
                        {renderField('orisId')}
                    </div>
                </div>
            )}

            {/* Step indicator and buttons */}
            <div className="space-y-4 pt-4 border-t border-gray-200">
                <div className="flex gap-2">
                    {[1, 2, 3].map((step) => (
                        <div key={step}
                             className={`flex-1 h-1 rounded ${step <= currentStep ? 'bg-primary' : 'bg-gray-300'}`}/>
                    ))}
                </div>

                <div className="flex gap-3 justify-between">
                    <div className="flex gap-3">
                        {currentStep > 1 && (
                            <Button
                                type="button"
                                variant="secondary"
                                onClick={handleBack}
                                disabled={formik.isSubmitting}
                            >
                                Zpět
                            </Button>
                        )}
                        {currentStep < 3 && (
                            <Button
                                type="button"
                                variant="primary"
                                onClick={handleNext}
                                disabled={formik.isSubmitting}
                            >
                                Další
                            </Button>
                        )}
                    </div>
                    <div className="flex gap-3">
                        {currentStep === 3 && (
                            <Button
                                type="button"
                                variant="primary"
                                onClick={handleSubmit}
                                loading={formik.isSubmitting}
                                disabled={formik.isSubmitting}
                            >
                                Odeslat
                            </Button>
                        )}
                        {renderField('cancel')}
                    </div>
                </div>
            </div>
        </div>
    );
};

const RegisterMemberFormButton = () => {
    return <HalFormButton
        name={"memberRegistrationsPost"}
        customLayout={<MultiStepMemberRegistrationForm/>}
    />;
}