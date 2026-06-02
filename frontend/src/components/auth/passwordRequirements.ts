import type {PasswordRequirement} from './PasswordStrengthIndicator';

const RULE_DEFINITIONS: Array<{id: string; label: string; test: (p: string) => boolean}> = [
    {id: 'length', label: 'Minimálně 12 znaků', test: (p) => p.length >= 12},
    {id: 'uppercase', label: 'Alespoň 1 velké písmeno', test: (p) => /[A-Z]/.test(p)},
    {id: 'lowercase', label: 'Alespoň 1 malé písmeno', test: (p) => /[a-z]/.test(p)},
    {id: 'digit', label: 'Alespoň 1 číslo', test: (p) => /\d/.test(p)},
    {id: 'special', label: 'Alespoň 1 speciální znak', test: (p) => /[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]/.test(p)},
];

export function buildPasswordRequirements(password: string): PasswordRequirement[] {
    return RULE_DEFINITIONS.map(({id, label, test}) => ({id, label, met: test(password)}));
}
