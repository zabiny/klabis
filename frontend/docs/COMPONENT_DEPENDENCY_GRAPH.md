# Frontend Component Dependency Graph

This document visualizes the dependency relationships between pages and UI components in the frontend application.

---

## 1. High-Level Application Structure

```mermaid
graph TD
    App["App.tsx<br/>(Entry Point)"]
    Auth["AuthProvider"]
    Theme["ThemeProvider"]
    ErrorBound["ErrorBoundary"]
    Layout["Layout.tsx<br/>(Main Layout)"]
    FormProvider["HalFormProvider"]
    PageLayout["HalFormsPageLayout<br/>(Form Display Wrapper)"]
    App --> Auth
    App --> Theme
    App --> ErrorBound
    ErrorBound --> Layout
    Layout --> FormProvider
    Layout --> PageLayout
    Layout --> Pages["Pages<br/>(Route Outlets)"]
```

---

## 2. Pages and Their Direct Component Dependencies

```mermaid
graph TD
    subgraph Pages["📄 Pages"]
        HomePage["HomePage"]
        MemberDetails["MemberDetailsPage"]
        MembersPage["MembersPage"]
        CalendarPage["CalendarPage"]
        EventsPage["EventsPage"]
        FinancesPage["FinancesPage"]
        GenericHal["GenericHalPage"]
        SandPlace["HalNavigatorPage"]
        NotFound["NotFoundPage"]
    end

    subgraph HALComponents["🔄 HAL Navigator Components"]
        HalFormDisplay["HalFormDisplay"]
        HalFormsSection["HalFormsSection"]
        HalLinksSection["HalLinksSection"]
        HalEmbeddedTable["HalEmbeddedTable"]
        HalFormButton["HalFormButton"]
    end

    subgraph UIComponents["🎨 UI Components"]
        Button["Button"]
        Card["Card"]
        Alert["Alert"]
        Modal["Modal"]
        Spinner["Spinner"]
        Badge["Badge"]
        ErrorDisplay["ErrorDisplay"]
    end

    subgraph DomainComponents["🏛️ Domain Components"]
        MemberDetailsCard["MemberDetailsCard"]
        MemberDetailsField["MemberDetailsField"]
        MemberName["MemberName"]
        EventType["EventType"]
        AddressFields["AddressFields"]
        ContactFields["ContactFields"]
    end

    MemberDetails --> MemberDetailsCard
    MemberDetails --> MemberDetailsField
    MemberDetails --> HalLinksSection
    MemberDetails --> HalFormsSection
    EventsPage --> HalEmbeddedTable
    EventsPage --> EventType
    EventsPage --> MemberName
    EventsPage --> HalLinksSection
    EventsPage --> HalFormsSection
    FinancesPage --> HalEmbeddedTable
    FinancesPage --> HalFormButton
    FinancesPage --> Spinner
    CalendarPage --> HalLinksSection
    CalendarPage --> HalFormsSection
    GenericHal --> HalLinksSection
    GenericHal --> HalFormsSection
    GenericHal --> Modal
    GenericHal --> Alert
    GenericHal --> Spinner
    SandPlace --> HalFormDisplay
```

---

## 3. HAL Forms Component Hierarchy

```mermaid
graph TD
    HalFormsPageLayout["HalFormsPageLayout<br/>(Layout Wrapper)"]
    HalFormButton["HalFormButton<br/>(Trigger Button)"]
    HalFormsSection["HalFormsSection<br/>(Lists Templates)"]
    HalFormDisplay["HalFormDisplay<br/>(Form Renderer)"]
    HalFormsForm["HalFormsForm<br/>(Core Form Component)"]
    KlabisFieldFactory["KlabisFieldFactory<br/>(Domain-Aware Factory)"]

    subgraph FieldComponents["Field Components"]
        Input["HalFormsInput"]
        Boolean["HalFormsBoolean"]
        Checkbox["HalFormsCheckbox"]
        CheckboxGroup["HalFormsCheckboxGroup"]
        Radio["HalFormsRadio"]
        Select["HalFormsSelect"]
        TextArea["HalFormsTextArea"]
        DateTime["HalFormsDateTime"]
        MemberId["HalFormsMemberId<br/>(Custom)"]
    end

    ErrorDisplay["ErrorDisplay"]
    Spinner["Spinner"]
    ModalOverlay["ModalOverlay"]
    HalFormsPageLayout --> HalFormButton
    HalFormsPageLayout --> HalFormsSection
    HalFormsPageLayout --> HalFormDisplay
    HalFormsPageLayout --> ModalOverlay
    HalFormsSection --> HalFormButton
    HalFormButton --> HalFormDisplay
    HalFormDisplay --> HalFormsForm
    HalFormDisplay --> ErrorDisplay
    HalFormDisplay --> Spinner
    HalFormsForm --> KlabisFieldFactory
    KlabisFieldFactory --> Input
    KlabisFieldFactory --> Boolean
    KlabisFieldFactory --> Checkbox
    KlabisFieldFactory --> CheckboxGroup
    KlabisFieldFactory --> Radio
    KlabisFieldFactory --> Select
    KlabisFieldFactory --> TextArea
    KlabisFieldFactory --> DateTime
    KlabisFieldFactory --> MemberId
```

---

## 4. UI Component Library Dependencies

```mermaid
graph TD
    subgraph BasicUI["Basic UI Components"]
        Button["Button"]
        Card["Card"]
        Alert["Alert"]
        Badge["Badge"]
        Spinner["Spinner"]
        Skeleton["Skeleton"]
        Toast["Toast"]
    end

    subgraph LayoutUI["Layout Components"]
        AppBar["AppBar"]
        Box["Box"]
        Container["Container"]
        Grid["Grid"]
    end

    subgraph FormUI["Form Components"]
        TextField["TextField"]
        SelectField["SelectField"]
        CheckboxField["CheckboxField"]
        RadioGroup["RadioGroup"]
        SwitchField["SwitchField"]
        FieldWrapper["FieldWrapper"]
        FormControl["FormControl"]
    end

    subgraph DialogUI["Dialog & Modal"]
        Modal["Modal"]
        ModalOverlay["ModalOverlay"]
        ErrorDisplay["ErrorDisplay"]
    end

    Icons["Icon Components<br/>(Folder: UI/icons/)"]
    FieldWrapper --> TextField
    FieldWrapper --> SelectField
    FieldWrapper --> CheckboxField
    FormControl --> FieldWrapper
```

---

## 5. Context & State Management Dependencies

```mermaid
graph TD
    subgraph Contexts["📦 Context Providers"]
        HalRouteCtx["HalRouteContext<br/>useHalRoute()"]
        HalFormCtx["HalFormContext<br/>useHalForm()"]
        AuthCtx["AuthContext2<br/>useAuth()"]
    end

    subgraph Hooks["🪝 Custom Hooks"]
        UseHalRoute["useHalRoute"]
        UseHalActions["useHalActions"]
        UseHalFormData["useHalFormData"]
        UseHalFormOptions["useHalFormOptions"]
        UseAuthorizedFetch["useAuthorizedFetch"]
        UseRootNavigation["useRootNavigation"]
        UseIsAdmin["useIsAdmin"]
        UseFormCacheInvalidation["useFormCacheInvalidation"]
    end

    subgraph Components["Components Using Hooks"]
        HalLinksSection["HalLinksSection"]
        HalFormsSection["HalFormsSection"]
        HalFormDisplay["HalFormDisplay"]
        Pages["Pages<br/>(Most Pages)"]
    end

    HalRouteCtx --> UseHalRoute
    HalFormCtx --> UseHalFormData
    AuthCtx --> UseIsAdmin
    UseHalRoute --> HalLinksSection
    UseHalRoute --> HalFormsSection
    UseHalRoute --> Pages
    UseHalFormOptions --> HalFormDisplay
    UseAuthorizedFetch --> Components
```

---

## 6. Member Domain Components Hierarchy

```mermaid
graph TD
    MemberDetailsPage["MemberDetailsPage"]

    subgraph MemberComponents["Member-Specific Components"]
        MemberDetailsCard["MemberDetailsCard<br/>(Section Container)"]
        MemberDetailsField["MemberDetailsField<br/>(Field Display)"]
        MemberName["MemberName"]
        AddressFields["AddressFields"]
        ContactFields["ContactFields"]
        GuardiansFields["GuardiansFields"]
    end

    KlabisFieldFactory["KlabisFieldsFactory<br/>(Domain-Aware Field Mapper)"]

    subgraph HALFields["HAL Field Components"]
        HalInput["HalFormsInput"]
        HalSelect["HalFormsSelect"]
        HalBoolean["HalFormsBoolean"]
        HalCheckboxGroup["HalFormsCheckboxGroup"]
    end

    UI["UI Components<br/>(Button, Card, etc.)"]
    MemberDetailsPage --> MemberDetailsCard
    MemberDetailsPage --> MemberDetailsField
    MemberDetailsPage --> MemberName
    MemberDetailsCard --> MemberDetailsField
    MemberDetailsCard --> AddressFields
    MemberDetailsCard --> ContactFields
    KlabisFieldFactory --> HalInput
    KlabisFieldFactory --> HalSelect
    KlabisFieldFactory --> HalBoolean
    KlabisFieldFactory --> HalCheckboxGroup
    MemberDetailsField --> UI
```

---

## 7. Table Components Hierarchy

```mermaid
graph TD
    HalEmbeddedTable["HalEmbeddedTable<br/>(HAL Embedded Data)"]
    KlabisTable["KlabisTable<br/>(Core Table)"]
    KlabisTableWithQuery["KlabisTableWithQuery<br/>(With Data Fetching)"]

    subgraph TableParts["Table Parts"]
        TableCell["TableCell"]
        Pagination["Pagination"]
        TableTypes["types.ts"]
    end

    UI["UI Components<br/>(Button, Icon, etc.)"]
    HalEmbeddedTable --> KlabisTable
    KlabisTableWithQuery --> KlabisTable
    KlabisTable --> TableCell
    KlabisTable --> Pagination
    KlabisTable --> UI
```

---

## 8. Complete Component Dependency Tree (Flattened)

```mermaid
graph TD
    App["App.tsx"]
    App --> Layout["Layout.tsx"]
    Layout --> ThemeToggle["ThemeToggle"]
    Layout --> FormProvider["HalFormProvider"]
    Layout --> PageLayout["HalFormsPageLayout"]
    Layout --> LogoutIcon["LogoutIcon"]
    Layout --> Button["Button"]
    PageLayout --> ModalOverlay["ModalOverlay"]
    PageLayout --> HalFormDisplay["HalFormDisplay"]
    HalFormDisplay --> HalFormsForm["HalFormsForm"]
    HalFormDisplay --> ErrorDisplay["ErrorDisplay"]
    HalFormDisplay --> Spinner["Spinner"]
    HalFormsForm --> KlabisFieldFactory["KlabisFieldFactory"]
    KlabisFieldFactory --> FieldComponents["Field Components<br/>(8 types)"]
    Layout --> MemberDetailsPage["MemberDetailsPage"]
    MemberDetailsPage --> MemberDetailsCard["MemberDetailsCard"]
    MemberDetailsCard --> MemberDetailsField["MemberDetailsField"]
    MemberDetailsPage --> HalLinksSection["HalLinksSection"]
    MemberDetailsPage --> HalFormsSection["HalFormsSection"]
    HalFormsSection --> HalFormButton["HalFormButton"]
    HalFormButton --> HalFormDisplay
    Layout --> EventsPage["EventsPage"]
    EventsPage --> HalEmbeddedTable["HalEmbeddedTable"]
    EventsPage --> EventType["EventType"]
    EventsPage --> MemberName["MemberName"]
    EventsPage --> HalLinksSection
    EventsPage --> HalFormsSection
    Layout --> FinancesPage["FinancesPage"]
    FinancesPage --> HalEmbeddedTable
    FinancesPage --> HalFormButton
    Layout --> GenericHalPage["GenericHalPage"]
    GenericHalPage --> Modal["Modal"]
    GenericHalPage --> Alert["Alert"]
    GenericHalPage --> HalLinksSection
    GenericHalPage --> HalFormsSection
    style App fill: #ff9999
    style Layout fill: #ffcc99
    style MemberDetailsPage fill: #99ccff
    style EventsPage fill: #99ccff
    style GenericHalPage fill: #99ccff
    style HalFormsForm fill: #99ff99
    style KlabisFieldFactory fill: #99ff99
```

---

## 9. Data Flow: How Forms Work

```mermaid
sequenceDiagram
    participant User
    participant Button as HalFormButton
    participant Context as HalFormContext
    participant Layout as HalFormsPageLayout
    participant Display as HalFormDisplay
    participant Form as HalFormsForm
    participant Factory as KlabisFieldFactory
    User ->> Button: Click Form Button
    Button ->> Context: Request Form Display
    Context ->> Layout: Notify (Query Param)
    Layout ->> Display: Render Form
    Display ->> Form: Provide Form Data
    Form ->> Factory: Create Field Components
    Factory ->> Form: Return Field Components
    Form -->> Display: Render Fields
    Display -->> User: Show Form
```

---

## 10. Data Flow: Route Navigation

```mermaid
sequenceDiagram
    participant User
    participant Router as React Router
    participant HalRoute as HalRouteContext
    participant API as Backend API
    participant Page as Page Component
    participant Section as HalLinksSection/Form
    User ->> Router: Navigate to /members/123
    Router ->> HalRoute: Activate Provider
    HalRoute ->> API: Fetch /api/members/123
    API -->> HalRoute: Return HAL Resource
    HalRoute ->> Page: Provide via useHalRoute()
    Page ->> Section: Pass Resource Data
    Section -->> User: Render Links & Forms
```

---

## 11. Component Reusability Matrix

| Component             | Used In                                                 | Times | Reusability |
|-----------------------|---------------------------------------------------------|-------|-------------|
| **HalFormsSection**   | MemberDetails, CalendarPage, EventsPage, GenericHalPage | 4+    | ⭐⭐⭐⭐⭐       |
| **HalLinksSection**   | MemberDetails, CalendarPage, EventsPage, GenericHalPage | 4+    | ⭐⭐⭐⭐⭐       |
| **HalEmbeddedTable**  | EventsPage, FinancesPage                                | 2+    | ⭐⭐⭐⭐        |
| **HalFormDisplay**    | HalFormsPageLayout, HalNavigatorPage                    | 2+    | ⭐⭐⭐⭐        |
| **Button**            | Layout, Pages, Forms, Tables                            | 10+   | ⭐⭐⭐⭐⭐       |
| **Spinner**           | HalFormDisplay, Multiple Pages                          | 5+    | ⭐⭐⭐⭐⭐       |
| **Modal**             | GenericHalPage, HalFormsPageLayout                      | 2+    | ⭐⭐⭐⭐        |
| **Card**              | MemberDetailsCard, Pages                                | 3+    | ⭐⭐⭐⭐        |
| **MemberDetailsCard** | MemberDetailsPage                                       | 1     | ⭐⭐          |
| **EventType**         | EventsPage                                              | 1     | ⭐⭐          |

---

## 12. Import Dependency Statistics

### UI Component Library

- **21+ UI components** in `/src/components/UI/`
- **8+ Icon components** in `/src/components/UI/icons/`
- **20+ Form field components** in UI and HAL Forms

### HAL Navigator Components

- **9 core HAL components** (display, forms, sections)
- **8 field type components** (input, select, boolean, etc.)
- **2 utility files** (types, utils)

### Domain-Specific Components

- **6 member components** (details, fields, name)
- **5 table components** (core, query, cell, pagination)
- **1 event component** (event type)

### Contexts & Hooks

- **3 context providers**
- **8 custom hooks**

### Pages

- **9 main pages**
- **1 layout wrapper** (applies to all pages)

---

## 13. Code Organization Best Practices

### Current Structure ✅

```
frontend/src/
├── components/
│   ├── HalNavigator2/          # HAL protocol implementation
│   │   ├── halforms/           # Form components & fields
│   │   └── *.tsx               # Display components
│   ├── UI/                      # Reusable UI library
│   │   ├── forms/              # Form input components
│   │   ├── layout/             # Layout components
│   │   ├── icons/              # Icon components
│   │   └── *.tsx               # Basic components
│   ├── members/                # Domain: Member
│   ├── events/                 # Domain: Events
│   ├── KlabisTable/            # Domain: Tables
│   ├── ThemeToggle/            # Theme toggle
│   ├── KlabisFieldsFactory.tsx # Domain field mapper
│   ├── JsonPreview.tsx         # Dev utility
│   └── ErrorFallback.tsx       # Error boundary
├── contexts/                    # State management
├── hooks/                       # Custom hooks
├── pages/                       # Page routes
└── styles/                      # Global styles
```

### Key Insights 💡

1. **Separation of Concerns**: UI, HAL, and Domain components are clearly separated
2. **Reusable Patterns**: HalLinksSection and HalFormsSection used across multiple pages
3. **Factory Pattern**: KlabisFieldsFactory extends HAL Forms with domain knowledge
4. **Context-Driven**: Most pages use useHalRoute() for automatic data fetching
5. **Composability**: Small, focused components compose into larger features

---

## 14. Dependency Summary

### Most Connected Components (Hub Nodes)

1. **HalFormsPageLayout** - Used by all pages for form display
2. **HalLinksSection** - Used by 4+ pages for link display
3. **HalFormsSection** - Used by 4+ pages for form listings
4. **Button** - Used by 10+ components
5. **Spinner** - Used by 5+ components

### Deepest Dependency Chains

- **Layout → HalFormsPageLayout → HalFormDisplay → HalFormsForm → KlabisFieldFactory → 8 Field Types** (6 levels)
- **MemberDetailsPage → MemberDetailsCard → MemberDetailsField → UI Components** (4 levels)
- **EventsPage → HalEmbeddedTable → KlabisTable → TableCell → UI Components** (5 levels)

### Most Isolated Components

- **JsonPreview** - Dev/debug only
- **NotFoundPage** - Standalone page
- **ThemeToggle** - Single purpose

---

## 15. Future Optimization Opportunities

### Completed Refactoring ✅

1. **`useHalPageData` hook** - Implemented in `src/hooks/useHalPageData.ts`. Consolidates route + actions + admin checks
   into a single hook API with helper methods. Used by GenericHalPage.

### Potential Refactoring 🔧

1. Extract common page patterns into a `usePageLayout` hook
2. Consolidate form/link/table display into a `HalResource` display component
3. Create reusable "detail card" pattern for other domains (Events, Finance, etc.)

### Performance Monitoring 📊

1. Monitor re-renders of HalFormsPageLayout (high-traffic component)
2. Optimize field factory to memoize field components
3. Consider virtualization for large HalEmbeddedTable instances
4. Cache HAL resource responses at context level

---

**Generated on 2025-12-31**
**For questions about architecture, see the [Developers Guide](../developers.md)**
