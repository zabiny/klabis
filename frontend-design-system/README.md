# @klabis/design-system

Sdílený design systém pro Klabis frontend — React komponenty postavené na Tailwind CSS, nahrazující dřívější závislost na MUI.

## Instalace a použití

Balíček je součástí npm workspace v tomto repozitáři (`frontend-design-system/`), konzumuje ho `frontend/`.

```ts
import {Button, Badge, KlabisTable} from '@klabis/design-system'
import '@klabis/design-system/styles.css'
```

V `tailwind.config.ts` konzumující aplikace zapoj sdílený preset (definuje sémantické barvy, dark mode přes třídu `dark`, spacing apod.):

```ts
import klabisPreset from '@klabis/design-system/tailwind-preset'

export default {
    presets: [klabisPreset],
    content: [
        './src/**/*.{ts,tsx}',
        './node_modules/@klabis/design-system/dist/**/*.js',
    ],
}
```

## Vývoj

```bash
npm run storybook        # Storybook dev server, http://localhost:6006
npm run build             # produkční build (tsc + vite), výstup do dist/
npm run build-storybook   # statický build Storybooku
npm run typecheck         # kontrola typů bez emitu
npm run lint               # ESLint (včetně pravidel pro Storybook stories)
npm run test               # Vitest
```

## Komponenty

### Root (`src/components/`)

Alert, Badge, Button, Card, DetailRow, ErrorDisplay, FulltextSearchInput, Modal, PillGroup, Skeleton, Spinner, Toast, Tooltip

### Formuláře (`src/components/forms/`)

FieldWrapper, FormControl, FormHelperText, TextField, TextAreaField, SelectField, RadioGroup, CheckboxField, CheckboxGroup, SwitchField

### Layout (`src/components/layout/`)

Box, Container, Grid, AppBar, Toolbar

### Tabulka (`src/components/table/`)

KlabisTable, TableCell, CardView (mobilní zobrazení), Pagination

### Ikony (`src/components/icons/`)

ComputerDesktopIcon, DeleteIcon, EditIcon, LogoutIcon, MoonIcon, NewspaperIcon, SunIcon, SyncIcon
