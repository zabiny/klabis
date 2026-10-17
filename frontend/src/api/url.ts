// Kept free of imports: klabisUserManager evaluates normalizeUrl at module load, so any dependency here risks an import cycle
/**
 * Converts relative URL to absolute using current window.location's hostname. Absolute URL is left unchanged
 * @param input
 */
export const normalizeUrl = (input: string): string => {
    if (input.startsWith("/")) {
        // relative paths - normalize against current location URL
        const result = new URL(window.location.toString());
        result.pathname = input;
        result.search = "";
        return result.toString();
    } else {
        // presumably absolute path, return unchanged
        return input;
    }
}
