// src/env.d.ts
/// <reference types="vite/client" />

interface ImportMetaEnv {
    readonly VITE_HAL_ROOT_URI?: string;
}

interface ImportMeta {
    readonly env: ImportMetaEnv;
}