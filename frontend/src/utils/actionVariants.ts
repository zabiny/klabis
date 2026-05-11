/**
 * Maps HAL affordance template/link relation names to Button variant tokens.
 *
 * Policy (per spec review-1-6-registration-prefill-and-action-colors K2):
 * - primary-ghost → positive/constructive actions (register, publish); transparent bg, primary text
 * - warning-ghost → reversible destructive actions (unregister — member can re-register); transparent bg, warning text
 * - danger-ghost  → irreversible destructive actions (cancel event); transparent bg, red text
 * - ghost         → neutral/safe actions (edit, sync) and unknown fallback; transparent bg, default text
 *
 * Ghost variants keep the same visual weight as icon-only table action buttons (as in MembersPage)
 * while preserving semantic color differentiation via text/icon color only — no filled background.
 * When adding a new affordance, pick the variant based on the semantic impact
 * of the action on the user's data, not on its label.
 */

import type {ButtonVariant} from '../components/UI/Button';

const ACTION_VARIANT_MAP: Record<string, ButtonVariant> = {
    registerForEvent: 'primary-ghost',
    publishEvent: 'primary-ghost',
    newRegistration: 'primary-ghost',
    unregisterFromEvent: 'warning-ghost',
    cancelEvent: 'danger-ghost',
    updateEvent: 'ghost',
    syncEventFromOris: 'ghost',
};

export const getActionVariant = (affordanceName: string): ButtonVariant =>
    ACTION_VARIANT_MAP[affordanceName] ?? 'ghost';
