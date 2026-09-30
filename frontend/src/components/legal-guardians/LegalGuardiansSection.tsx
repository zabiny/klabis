import type {ReactElement} from 'react';
import {useNavigate} from 'react-router-dom';
import {Card, DetailRow} from '../UI';
import {useLegalGuardians} from '../../hooks/useLegalGuardians.ts';
import {toHref} from '../../api/hateoas.ts';
import {extractNavigationPath} from '../../utils/navigationPath.ts';
import type {HalResourceLinks} from '../../api';
import {labels} from '../../localization';

interface LegalGuardiansSectionProps {
    guardiansLink?: HalResourceLinks;
}

export const LegalGuardiansSection = ({guardiansLink}: LegalGuardiansSectionProps): ReactElement => {
    const {guardians} = useLegalGuardians(guardiansLink);
    const navigate = useNavigate();

    return (
        <Card className="p-6">
            <h3 className="text-xs uppercase font-semibold text-text-secondary mb-4">
                {labels.sections.legalGuardians}
            </h3>
            {guardians.length === 0 ? (
                <p className="text-sm text-text-secondary">{labels.ui.noLegalGuardians}</p>
            ) : (
                <dl>
                    {guardians.map(guardian => {
                        const link = (guardian._links?.member ?? guardian._links?.legalGuardian) as HalResourceLinks | undefined;
                        const name = `${guardian.firstName ?? ''} ${guardian.lastName ?? ''}`.trim() || guardian.userId;
                        return (
                            <DetailRow key={guardian.userId} label="">
                                <div className="flex flex-col">
                                    {link ? (
                                        <button type="button" className="text-left hover:underline text-text-primary"
                                                onClick={() => navigate(extractNavigationPath(toHref(link)))}>
                                            {name}
                                        </button>
                                    ) : (
                                        <span className="text-text-primary">{name}</span>
                                    )}
                                    {guardian.email && <span className="text-sm text-text-secondary">{guardian.email}</span>}
                                    {guardian.phone && <span className="text-sm text-text-secondary">{guardian.phone}</span>}
                                </div>
                            </DetailRow>
                        );
                    })}
                </dl>
            )}
        </Card>
    );
};
