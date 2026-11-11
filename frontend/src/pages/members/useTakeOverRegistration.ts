import {useEffect, useRef} from 'react';
import {useLocation, useNavigate} from 'react-router-dom';
import type {HalFormsTemplate} from '../../api';
import {defaultDialogTitle} from '../../components/HalNavigator2/useTemplateDialog.tsx';
import {useHalForm} from '../../contexts/halFormContext.ts';
import {renderRegistrationForm} from './RegistrationForm.tsx';

export interface TakeOverState {
    takeOverLegalGuardianId?: string;
    returnTo?: string;
}

export const useTakeOverRegistration = (registerMemberTemplate: HalFormsTemplate | undefined): void => {
    const location = useLocation();
    const navigate = useNavigate();
    const {displayHalForm} = useHalForm();
    const takeOverHandled = useRef(false);

    useEffect(() => {
        const {takeOverLegalGuardianId, returnTo} = (location.state ?? {}) as TakeOverState;
        if (takeOverHandled.current || !takeOverLegalGuardianId || !registerMemberTemplate) return;
        takeOverHandled.current = true;
        displayHalForm({
            templateName: 'registerMember',
            children: renderRegistrationForm(takeOverLegalGuardianId),
            dialogTitle: defaultDialogTitle('registerMember', registerMemberTemplate),
            onCancel: returnTo ? () => navigate(returnTo, {replace: true}) : undefined,
        });
        navigate(location.pathname + location.search, {replace: true, state: null});
        // Consumed once (takeOverHandled); re-running on every render's new callbacks would only be noise.
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [location.state, registerMemberTemplate]);
};
