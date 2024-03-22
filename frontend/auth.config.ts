import { defineConfig } from 'auth-astro'
import type { OIDCConfig } from '@auth/core/providers'
import type { Profile } from '@auth/core/types'
import GitHub from '@auth/core/providers/github'

export default defineConfig({
  trustHost: true,
  providers: [
    GitHub({
      clientId: import.meta.env.GITHUB_CLIENT_ID,
      clientSecret: import.meta.env.GITHUB_CLIENT_SECRET,
    }),
    // {
    //   id: "klabis-id",
    //   name: "KlabisId",
    //   type: "oidc",
    //   issuer: "https://my.oidc-provider.com",
    //   clientId: process.env.CLIENT_ID,
    //   clientSecret: process.env.CLIENT_SECRET,
    // } satisfies OIDCConfig<Profile>,
  ],
})
