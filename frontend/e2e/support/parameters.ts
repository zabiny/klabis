import {defineParameterType} from 'playwright-bdd';
import {localIsoDate} from './dates';

const resolvePlaceholders = (text: string) => {
    const prefix = `E2E-${process.env.E2E_RUN_ID}-${process.env.TEST_WORKER_INDEX}-`;
    return text
        .replaceAll('{prefix}', prefix)
        .replace(/\{date([+-]\d+)\}/g, (_, days) => localIsoDate(Number(days)));
};

defineParameterType({
    name: 'text',
    regexp: /"([^"]*)"/,
    transformer: resolvePlaceholders,
});
