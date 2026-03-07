import {type ReactElement} from 'react';
import {useNavigate} from 'react-router-dom';
import {Alert, Badge, Button, Spinner} from '../../components/UI';
import {MemberDetailsCard} from '../../components/members/MemberDetailsCard.tsx';
import {MemberDetailsField} from '../../components/members/MemberDetailsField.tsx';
import {extractNavigationPath} from '../../utils/navigationPath.ts';
import {useHalPageData} from '../../hooks/useHalPageData.ts';
import {formatDate} from "../../utils/dateUtils.ts";
import {HalFormButton} from "../../components/HalNavigator2/HalFormButton.tsx";

export const MemberDetailsPage = (): ReactElement => {
    const {resourceData, isLoading, error, route} = useHalPageData();
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
                    <p>Nepodařilo se načíst detaily člena z {route.pathname}</p>
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
        <div className="flex flex-col gap-6">
            {/* Back link */}
            <button
                onClick={() => navigate(-1)}
                className="text-sm text-primary hover:text-primary-hover transition-colors self-start"
            >
                ← Zpět na seznam
            </button>

            {/* Header: Name + Registration Number + Status Badge */}
            <div className="flex items-center justify-between">
                <div className="flex items-center gap-4">
                    <h1 className="text-2xl font-bold text-text-primary">
                        {member.firstName} {member.lastName}
                    </h1>
                    {member.registrationNumber && (
                        <span className="text-lg text-text-secondary">{member.registrationNumber}</span>
                    )}
                    {member.active !== undefined && (
                        <Badge variant={member.active ? 'success' : 'default'} size="sm">
                            {member.active ? 'Aktivní' : 'Neaktivní'}
                        </Badge>
                    )}
                </div>
            </div>

            {/* Action buttons from HAL affordances */}
            <div className="flex gap-3">
                <HalFormButton name="editOwnMemberInfoPatch"/>
                <HalFormButton name="editByAdminPatch"/>
                {member._links?.permissions && (
                    <Button
                        onClick={() => handleNavigateToLink(member._links.permissions.href)}
                        variant="secondary"
                        size="sm"
                    >
                        Správa oprávnění
                    </Button>
                )}
                <HalFormButton name="terminatePatch"/>
                <HalFormButton name="reactivatePatch"/>
            </div>

            {/* Detail sections - vertical layout */}
            <div className="flex flex-col gap-6">
                {/* Osobní údaje */}
                <MemberDetailsCard title="Osobní údaje">
                    <div className="space-y-0">
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
                            render={(val) => val === 'male' ? 'Muž' : val === 'female' ? 'Žena' : val}
                        />
                        <MemberDetailsField label="Státní příslušnost" value={member.nationality}/>
                        {member.birthCertificateNumber && (
                            <MemberDetailsField label="Rodné číslo" value={member.birthCertificateNumber}/>
                        )}
                    </div>
                </MemberDetailsCard>

                {/* Kontakt */}
                {member.contact && (
                    <MemberDetailsCard title="Kontakt">
                        <div className="space-y-0">
                            {member.contact.email && (
                                <MemberDetailsField
                                    label="E-mail"
                                    value={member.contact.email}
                                    render={(val) => <a href={`mailto:${val}`}
                                                        className="text-primary hover:text-primary-hover transition-colors">{val}</a>}
                                />
                            )}
                            {member.contact.phone && (
                                <MemberDetailsField
                                    label="Telefon"
                                    value={member.contact.phone}
                                    render={(val) => <a href={`tel:${val}`}
                                                        className="text-primary hover:text-primary-hover transition-colors">{val}</a>}
                                />
                            )}
                            {member.contact.note && (
                                <MemberDetailsField label="Poznámka" value={member.contact.note}/>
                            )}
                        </div>
                    </MemberDetailsCard>
                )}

                {/* Adresa */}
                {member.address && (
                    <MemberDetailsCard title="Adresa">
                        <div className="space-y-0">
                            <MemberDetailsField label="Ulice" value={member.address.streetAndNumber}/>
                            <MemberDetailsField label="Město" value={member.address.city}/>
                            <MemberDetailsField label="PSČ" value={member.address.postalCode}/>
                            <MemberDetailsField label="Stát" value={member.address.country}/>
                        </div>
                    </MemberDetailsCard>
                )}

                {/* Doplňkové informace */}
                {(member.siCard || member.bankAccount || member.dietaryRestrictions) && (
                    <MemberDetailsCard title="Doplňkové informace">
                        <div className="space-y-0">
                            {member.siCard && (
                                <MemberDetailsField label="Číslo čipu" value={member.siCard}/>
                            )}
                            {member.bankAccount && (
                                <MemberDetailsField label="Číslo bankovního účtu" value={member.bankAccount}/>
                            )}
                            {member.dietaryRestrictions && (
                                <MemberDetailsField label="Stravovací omezení" value={member.dietaryRestrictions}/>
                            )}
                        </div>
                    </MemberDetailsCard>
                )}

                {/* Doklady a licence */}
                {(member.identityCard || member.drivingLicence || member.medicCourse !== undefined || member.licences) && (
                    <MemberDetailsCard title="Doklady a licence">
                        <div className="space-y-0">
                            {member.identityCard?.number && (
                                <MemberDetailsField
                                    label="Občanský průkaz"
                                    value={member.identityCard}
                                    render={(val) => (
                                        <span>
                                            {val.number}
                                            {val.expiryDate && <span className="text-text-secondary"> (platný do: {formatDate(val.expiryDate)})</span>}
                                        </span>
                                    )}
                                />
                            )}
                            {member.drivingLicence && member.drivingLicence.length > 0 && (
                                <MemberDetailsField
                                    label="Řidičský průkaz"
                                    value={member.drivingLicence.join(', ')}
                                />
                            )}
                            {member.medicCourse !== undefined && (
                                <MemberDetailsField
                                    label="Zdravotní kurz"
                                    value={member.medicCourse}
                                />
                            )}
                            {member.licences?.trainer && (
                                <MemberDetailsField
                                    label="Trénérská licence"
                                    value={member.licences.trainer}
                                    render={(val) => (
                                        <span>
                                            {val.licence}
                                            {val.expiryDate && <span className="text-text-secondary"> (platná do: {formatDate(val.expiryDate)})</span>}
                                        </span>
                                    )}
                                />
                            )}
                            {member.licences?.referee && (
                                <MemberDetailsField
                                    label="Rozhodčí licence"
                                    value={member.licences.referee}
                                    render={(val) => (
                                        <span>
                                            {val.licence}
                                            {val.expiryDate && <span className="text-text-secondary"> (platná do: {formatDate(val.expiryDate)})</span>}
                                        </span>
                                    )}
                                />
                            )}
                            {member.licences?.ob && (
                                <MemberDetailsField label="OB Licence" value={member.licences.ob.licence}/>
                            )}
                        </div>
                    </MemberDetailsCard>
                )}

                {/* Zákonní zástupci */}
                {member.legalGuardians && member.legalGuardians.length > 0 && (
                    <MemberDetailsCard title="Zákonný zástupce">
                        <div className="space-y-6">
                            {member.legalGuardians.map((guardian: any, idx: number) => (
                                <div key={idx}
                                     className="border-t border-border pt-4 first:border-0 first:pt-0">
                                    <h4 className="font-semibold mb-3">{guardian.firstName} {guardian.lastName}</h4>
                                    <div className="space-y-0 ml-4">
                                        {guardian.contact?.email && (
                                            <MemberDetailsField
                                                label="E-mail"
                                                value={guardian.contact.email}
                                                render={(val) => <a href={`mailto:${val}`}
                                                                    className="text-primary hover:text-primary-hover transition-colors">{val}</a>}
                                            />
                                        )}
                                        {guardian.contact?.phone && (
                                            <MemberDetailsField
                                                label="Telefon"
                                                value={guardian.contact.phone}
                                                render={(val) => <a href={`tel:${val}`}
                                                                    className="text-primary hover:text-primary-hover transition-colors">{val}</a>}
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
        </div>
    );
};
