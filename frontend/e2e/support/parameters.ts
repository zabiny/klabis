import {defineParameterType} from 'playwright-bdd';

const resolvePlaceholders = (text: string) => {
    const prefix = `E2E-${process.env.E2E_RUN_ID}-${process.env.TEST_WORKER_INDEX}-`;
    return text
        .replaceAll('{prefix}', prefix)
        .replace(/\{date\+(\d+)\}/g, (_, days) => {
            const date = new Date();
            date.setDate(date.getDate() + Number(days));
            return date.toISOString().slice(0, 10);
        });
};

defineParameterType({
    name: 'text',
    regexp: /"([^"]*)"/,
    transformer: resolvePlaceholders,
});
