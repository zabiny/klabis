/// <reference types="vite/client" />
/// <reference types="vite-plugin-pwa/react" />

interface ImportMetaEnv {
    readonly VITE_OAUTH_CLIENT_ID: string;
    readonly VITE_OAUTH_CLIENT_SECRET: string | undefined;
    readonly VITE_OAUTH_SCOPE: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}
