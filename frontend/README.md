# Klabis FE

- [astro](https://astro.build/)
- [auth.js](https://authjs.dev/)
- [shadcn](https://ui.shadcn.com/)
- [react](https://react.dev/)

deployed to cloudflare pages - [klabis.otakar.io](https://klabis.otakar.io)

### How to start

1. clone the repo
2. fill in the `.env` file with the following variables:

```
AUTH_SECRET= // 32 characters long random string, hint: openssl rand -base64 32
GITHUB_CLIENT_ID= // get it from github oauth
GITHUB_CLIENT_SECRET=
```

3. run `npm install`
4. run `npm run dev`

### 🚀 Astro Project Structure

Inside of your Astro project, you'll see the following folders and files:

```text
/
├── public/
│   └── favicon.svg
├── src/
│   ├── components/
│   │   └── Card.astro
│   ├── layouts/
│   │   └── Layout.astro
│   └── pages/
│       └── index.astro
└── package.json
```

Astro looks for `.astro` or `.md` files in the `src/pages/` directory. Each page is exposed as a route based on its file name.

There's nothing special about `src/components/`, but that's where we like to put any Astro/React/Vue/Svelte/Preact components.

Any static assets, like images, can be placed in the `public/` directory.

### 🧞 Commands

| Command       | Action                                      |
| :------------ | :------------------------------------------ |
| `npm install` | Installs dependencies                       |
| `npm run dev` | Starts local dev server at `localhost:4321` |
