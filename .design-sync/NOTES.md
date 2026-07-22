# Design-sync notes — @klabis/design-system

## Fixes

- `[GENERAL]` **All 31 components failed to render** with `TypeError: (0 , import_addon_themes.withThemeByClassName) is not a function`.
  Root cause: `.storybook/preview.tsx` decorates every story with `withThemeByClassName(...)` from `@storybook/addon-themes`.
  The converter's decorator bundle stubs `@storybook/*` packages via an inert proxy (`storybookStubPlugin` in
  `lib/story-imports.mjs`), whose named-export allowlist (`fn action actions expect userEvent ...`) didn't include
  `withThemeByClassName`/theme-addon factories, so the imported name resolved to `undefined` instead of the inert
  callable, and calling it as `withThemeByClassName({...})` threw.
  Fix: patched the staged `.ds-sync/lib/story-imports.mjs`'s `INERT_STUB` name list to add
  `withThemeByClassName withThemeByDataAttribute withThemeFromJSXProvider`, each stubbed as a factory returning an
  identity decorator (`function(){return function(Story){return Story()}}`) instead of the generic `inert` proxy —
  this addon only toggles a CSS class for light/dark theme (no real React context), so an identity decorator is
  functionally correct for preview purposes.
  **Note:** this fix lives in `.ds-sync/lib/story-imports.mjs` (the staged converter copy), NOT in
  `.design-sync/overrides/` — a `.design-sync/overrides/story-imports.mjs` fork does NOT take effect here because
  `source-storybook.mjs`'s decorator-bundling path statically imports `storybookStubPlugin` from its own sibling
  `./story-imports.mjs`, bypassing the `cfg.libOverrides` redirection (which only applies to modules `package-build.mjs`
  itself loads via `loadLib()`). **Re-copying the skill's staged scripts on a future re-sync (`.ds-sync/`) will wipe
  this fix** — re-apply it (or escalate to the skill's stub allowlist) before rebuilding on a fresh clone/re-sync.

- `Icons` story title excluded via `titleMap: {"Icons": null}` — it's a documentation/gallery story
  (`src/components/icons/Icons.stories.tsx`) that renders 8 separately-exported icon components
  (ComputerDesktopIcon, DeleteIcon, EditIcon, LogoutIcon, MoonIcon, NewspaperIcon, SunIcon, SyncIcon) side by side; it
  isn't itself a component export, so it can't pair to a bundle export.

- `[GRID_OVERFLOW]` on 12 components (Button, Card, DetailRow, FulltextSearchInput, Skeleton, CheckboxGroup,
  FormControl, TextField, AppBar, Box, Grid, Pagination) — stories render wider than the grid card cell (data
  tables, full-width bars, horizontal layouts). Fixed via `cfg.overrides.<Name>.cardMode: "column"`.

- `[GRID_OVERFLOW]` (escape) on Modal and Toast — dialog/toast stories position fixed/portal content outside any
  grid cell. Fixed via `cfg.overrides.<Name>.cardMode: "single"` with `primaryStory: "Default"`.

## Re-sync risks

- The `story-imports.mjs` stub patch (above) is NOT committed anywhere the skill's own re-sync tooling reads —
  it's a hand-edit of the staged `.ds-sync/lib/` copy, which is gitignored and gets wiped/replaced on every
  `cp -r <skill-base-dir>/... .ds-sync/` refresh. **On every future sync (first re-run on a fresh clone, or any
  re-sync), re-apply this same one-line edit to `.ds-sync/lib/story-imports.mjs` immediately after staging the
  scripts, before running the build** — otherwise every component fails to render again with the same
  `withThemeByClassName is not a function` error.
- Toolchain: npm workspaces monorepo (`klabis` root `package.json` workspaces: `frontend`, `frontend-design-system`).
  `react`/`react-dom` hoist to the **repo-root** `node_modules`, not `frontend-design-system/node_modules` — always
  pass `--node-modules ./node_modules` (repo root) to `package-build.mjs`/`package-validate.mjs`/`resync.mjs`, and
  `--entry ./frontend-design-system/dist/index.js` (package.json `module`/`exports['.']` point at the built dist,
  already correct — no override needed there).
- `[FONT_REMOTE]` warning is expected/accepted: `Plus Jakarta Sans`, `JetBrains Mono`, `Space Grotesk` are served via
  a remote font-host `@import` in the compiled CSS (scraped from storybook's build) — not vendored locally. Not
  independently verified that the CDN is reachable from every future build environment; if fonts ever render as
  fallback in previews, check this first.
- `tokens: 89 defined, 58 referenced (1 missing, below threshold)` — one Tailwind design token is defined but never
  referenced by any component story; below the validator's warning threshold, not investigated further.
