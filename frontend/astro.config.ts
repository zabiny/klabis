import { defineConfig, passthroughImageService } from 'astro/config'
import react from '@astrojs/react'
import tailwind from '@astrojs/tailwind'
import auth from 'auth-astro'
import cloudflare from '@astrojs/cloudflare'
import svelte from '@astrojs/svelte'

// https://astro.build/config
export default defineConfig({
  output: 'server',
  adapter: cloudflare(),
  integrations: [
    react(),
    tailwind({
      applyBaseStyles: false,
    }),
    auth(),
    svelte(),
  ],
  image: {
    service: passthroughImageService(),
  },
})
