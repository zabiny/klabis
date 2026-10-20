import {test} from '@playwright/test';
import {defineParameterType} from 'playwright-bdd';
import {localIsoDate} from './dates';
import {personas, type Persona} from './personas';

export type Contribution = {ruleType: 'PERCENTAGE' | 'FIXED_AMOUNT'; value: number; display: string};

// Entities are never cleaned up and catalog levels are found by name, so every name is unique per run and scenario;
// otherwise a scenario would silently reuse a level another scenario created with a different yearly fee.
defineParameterType({
    name: 'name',
    regexp: /"([^"]*)"/,
    transformer: (name: string) => `E2E-${process.env.E2E_RUN_ID}-${test.info().testId.slice(-8)}-${name}`,
});

defineParameterType({
    name: 'persona',
    regexp: new RegExp(Object.keys(personas).join('|')),
    transformer: (persona: string) => persona as Persona,
});

defineParameterType({
    name: 'day',
    regexp: /today|yesterday|in \d+ days|\d+ days from today/,
    transformer: (day: string) => {
        if (day === 'today') return localIsoDate(0);
        if (day === 'yesterday') return localIsoDate(-1);
        return localIsoDate(Number(day.match(/\d+/)![0]));
    },
});

defineParameterType({
    name: 'contribution',
    regexp: /\d+ %|\d+ CZK/,
    transformer: (contribution: string): Contribution => {
        const value = Number(contribution.match(/\d+/)![0]);
        return contribution.endsWith('%')
            ? {ruleType: 'PERCENTAGE', value, display: `${value} %`}
            : {ruleType: 'FIXED_AMOUNT', value, display: `${value} CZK`};
    },
});
