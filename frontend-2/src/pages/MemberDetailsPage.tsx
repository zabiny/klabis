import {type ReactElement} from 'react';
import {useNavigate} from 'react-router-dom';
import {useHalRoute} from '../contexts/HalRouteContext';
import {Alert, Button, Spinner} from '../components/UI';
import {MemberDetailsCard} from '../components/members/MemberDetailsCard';
import {MemberDetailsField} from '../components/members/MemberDetailsField';
import {extractNavigationPath} from '../utils/navigationPath';
import {HalLinksSection} from "../components/HalLinksSection.tsx";
import {HalFormsSection} from "../components/HalFormsSection.tsx";
import {useHalActions} from "../hooks/useHalActions.ts";

/**
 * Format date string to readable format
 */
function formatDate(dateString: string | undefined): string {
    if (!dateString) return '—';
    try {
        return new Date(dateString).toLocaleDateString('cs-CZ');
    } catch {
        return dateString;
    }
}

/**
 * Page for displaying member details fetched from GET /members/{id} API
 */
export const MemberDetailsPage = (): ReactElement => {
    const {resourceData, isLoading, error, pathname} = useHalRoute();
    const {
        handleNavigateToItem,
        handleFormSubmit,
        selectedTemplate,
        setSelectedTemplate,
        submitError,
        isSubmitting
    } = useHalActions();
    const navigate = useNavigate();

    if (isLoading) {
        return (
            <div className="flex items-center justify-center py-12">
                <Spinner/>
            </div>
        );
    }

    if (error) {
        return (
            <Alert severity="error">
                <div className="space-y-2">
                    <p>Nepodařilo se načíst detaily člena z {pathname}</p>
                    <p className="text-sm text-gray-600">{error.message}</p>
                </div>
            </Alert>
        );
    }

    if (!resourceData) {
        return (
            <Alert severity="warning">
                <p>Žádná data dostupná</p>
            </Alert>
        );
    }

    const member = resourceData as any;

    const handleNavigateToLink = (href: string) => {
        const path = extractNavigationPath(href);
        navigate(path);
    };

    return (
        <div className="space-y-6">
            {/* Header Section */}
            <div className="flex items-center justify-between">
                <h1 className="text-3xl font-bold">
                    {member.firstName} {member.lastName}
                </h1>
                <div className="flex gap-2">
                    {member._links?.editOwnMemberInfoForm && (
                        <Button
                            onClick={() => handleNavigateToLink(member._links.editOwnMemberInfoForm.href)}
                            variant="primary"
                        >
                            Upravit profil
                        </Button>
                    )}
                    {member._links?.editByAdminForm && (
                        <Button
                            onClick={() => handleNavigateToLink(member._links.editByAdminForm.href)}
                            variant="primary"
                        >
                            Upravit (Admin)
                        </Button>
                    )}
                </div>
            </div>

            {/* Multi-column grid layout for information sections */}
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
                {/* Basic Info Section */}
                <MemberDetailsCard title="Základní informace">
                <div className="space-y-0">
                    <MemberDetailsField label="ID člena" value={member.id?.value}/>
                    <MemberDetailsField label="Registrační číslo" value={member.registrationNumber}/>
                    <MemberDetailsField label="Jméno" value={member.firstName}/>
                    <MemberDetailsField label="Příjmení" value={member.lastName}/>
                    <MemberDetailsField
                        label="Datum narození"
                        value={member.dateOfBirth}
                        render={(val) => formatDate(val)}
                    />
                    <MemberDetailsField
                        label="Pohlaví"
                        value={member.sex}
                        render={(val) => val === 'male' ? 'Muž' : 'Žena'}
                    />
                    <MemberDetailsField
                        label="Národnost"
                        value={member.nationality}
                    />
                </div>
            </MemberDetailsCard>

            {/* Identity Section */}
            {(member.identityCard || member.birthCertificateNumber) && (
                <MemberDetailsCard title="Identifikace">
                    <div className="space-y-0">
                        {member.identityCard?.number && (
                            <MemberDetailsField label="Číslo dokladu" value={member.identityCard.number}/>
                        )}
                        {member.identityCard?.expiryDate && (
                            <MemberDetailsField
                                label="Expiruje"
                                value={member.identityCard.expiryDate}
                                render={(val) => formatDate(val)}
                            />
                        )}
                        {member.birthCertificateNumber && (
                            <MemberDetailsField label="Číslo rodného listu" value={member.birthCertificateNumber}/>
                        )}
                    </div>
                </MemberDetailsCard>
            )}

            {/* Contact Information Section */}
            {(member.contact || member.address) && (
                <MemberDetailsCard title="Kontaktní informace">
                    <div className="space-y-0">
                        {member.contact?.email && (
                            <MemberDetailsField
                                label="Email"
                                value={member.contact.email}
                                render={(val) => <a href={`mailto:${val}`}
                                                    className="text-blue-600 hover:underline">{val}</a>}
                            />
                        )}
                        {member.contact?.phone && (
                            <MemberDetailsField
                                label="Telefon"
                                value={member.contact.phone}
                                render={(val) => <a href={`tel:${val}`}
                                                    className="text-blue-600 hover:underline">{val}</a>}
                            />
                        )}
                        {member.contact?.note && (
                            <MemberDetailsField label="Pozn. ke kontaktu" value={member.contact.note}/>
                        )}
                        {member.address && (
                            <>
                                <MemberDetailsField label="Ulice" value={member.address.streetAndNumber}/>
                                <MemberDetailsField label="Město" value={member.address.city}/>
                                <MemberDetailsField label="PSČ" value={member.address.postalCode}/>
                                <MemberDetailsField label="Země" value={member.address.country}/>
                            </>
                        )}
                    </div>
                </MemberDetailsCard>
            )}

            {/* Member Details Section */}
            {(member.siCard || member.bankAccount || member.dietaryRestrictions || member.medicCourse !== undefined) && (
                <MemberDetailsCard title="Údaje člena">
                    <div className="space-y-0">
                        {member.siCard && (
                            <MemberDetailsField label="SI čip číslo" value={member.siCard}/>
                        )}
                        {member.bankAccount && (
                            <MemberDetailsField label="Bankovní účet" value={member.bankAccount}/>
                        )}
                        {member.dietaryRestrictions && (
                            <MemberDetailsField label="Dietní omezení" value={member.dietaryRestrictions}/>
                        )}
                        {member.medicCourse !== undefined && (
                            <MemberDetailsField
                                label="Absolvent kurzu první pomoci"
                                value={member.medicCourse}
                            />
                        )}
                        {member.drivingLicence && member.drivingLicence.length > 0 && (
                            <MemberDetailsField
                                label="Řidičský průkaz"
                                value={member.drivingLicence.join(', ')}
                            />
                        )}
                    </div>
                </MemberDetailsCard>
            )}

            {/* Licenses Section */}
            {member.licences && (
                <MemberDetailsCard title="Licence">
                    <div className="space-y-0">
                        {member.licences.ob && (
                            <MemberDetailsField
                                label="OB Licence"
                                value={member.licences.ob.licence}
                            />
                        )}
                        {member.licences.referee && (
                            <>
                                <MemberDetailsField label="Rozhodčí licence" value={member.licences.referee.licence}/>
                                <MemberDetailsField
                                    label="Platnost do"
                                    value={member.licences.referee.expiryDate}
                                    render={(val) => formatDate(val)}
                                />
                            </>
                        )}
                        {member.licences.trainer && (
                            <>
                                <MemberDetailsField label="Trenérská licence" value={member.licences.trainer.licence}/>
                                <MemberDetailsField
                                    label="Platnost do"
                                    value={member.licences.trainer.expiryDate}
                                    render={(val) => formatDate(val)}
                                />
                            </>
                        )}
                    </div>
                </MemberDetailsCard>
            )}

            {/* Guardians Section */}
            {member.legalGuardians && member.legalGuardians.length > 0 && (
                <MemberDetailsCard title="Zákonní zástupci">
                    <div className="space-y-6">
                        {member.legalGuardians.map((guardian: any, idx: number) => (
                            <div key={idx}
                                 className="border-t border-gray-200 dark:border-gray-700 pt-4 first:border-0 first:pt-0">
                                <h4 className="font-semibold mb-3">{guardian.firstName} {guardian.lastName}</h4>
                                <div className="space-y-0 ml-4">
                                    {guardian.contact?.email && (
                                        <MemberDetailsField
                                            label="Email"
                                            value={guardian.contact.email}
                                            render={(val) => <a href={`mailto:${val}`}
                                                                className="text-blue-600 hover:underline">{val}</a>}
                                        />
                                    )}
                                    {guardian.contact?.phone && (
                                        <MemberDetailsField
                                            label="Telefon"
                                            value={guardian.contact.phone}
                                            render={(val) => <a href={`tel:${val}`}
                                                                className="text-blue-600 hover:underline">{val}</a>}
                                        />
                                    )}
                                    {guardian.note && (
                                        <MemberDetailsField label="Vztah" value={guardian.note}/>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </MemberDetailsCard>
            )}

            </div>

            {/* Links/Actions section */}
            {resourceData?._links && Object.keys(resourceData._links).length > 0 ? (
                <HalLinksSection
                    links={resourceData._links}
                    onNavigate={handleNavigateToItem}
                />
            ) : null}

            {/* Templates/Forms section */}
            {resourceData?._templates && Object.keys(resourceData._templates).length > 0 ? (
                <HalFormsSection
                    templates={resourceData._templates}
                    data={resourceData}
                    formState={{selectedTemplate, submitError, isSubmitting}}
                    handlers={{onSelectTemplate: setSelectedTemplate, onSubmit: handleFormSubmit}}
                />
            ) : null}

        </div>
    );
};
