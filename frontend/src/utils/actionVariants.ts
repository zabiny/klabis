/**
 * Maps HAL affordance template/link relation names to Button variant tokens.
 *
 * Policy (per spec review-1-6-registration-prefill-and-action-colors K2):
 * - primary   → positive/constructive actions (register, publish)
 * - warning   → reversible destructive actions (unregister — member can re-register)
 * - danger    → irreversible destructive actions (cancel event)
 * - secondary → neutral/safe actions (edit, sync) and unknown fallback
 *
 * When adding a new affordance, pick the variant based on the semantic impact
 * of the action on the user's data, not on its label.
 */

import type {ButtonVariant} from '../components/UI/Button';

const ACTION_VARIANT_MAP: Record<string, ButtonVariant> = {
    registerForEvent: 'primary',
    publishEvent: 'primary',
    newRegistration: 'primary',
    unregisterFromEvent: 'warning',
    cancelEvent: 'danger',
    updateEvent: 'secondary',
    syncEventFromOris: 'secondary',
};

export const getActionVariant = (affordanceName: string): ButtonVariant =>
    ACTION_VARIANT_MAP[affordanceName] ?? 'secondary';
