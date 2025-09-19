export function getNestedValue(obj: any, path: string): any {
    return path.split('.').reduce((current, key) => {
        return current && current[key] !== undefined ? current[key] : undefined;
    }, obj);
}

export function setNestedValue(obj: any, path: string, value: any): any {
    const keys = path.split('.');
    const result = {...obj};

    let current = result;
    for (let i = 0; i < keys.length - 1; i++) {
        const key = keys[i];
        if (!current[key] || typeof current[key] !== 'object') {
            current[key] = {};
        } else {
            current[key] = {...current[key]};
        }
        current = current[key];
    }

    current[keys[keys.length - 1]] = value;
    return result;
}
