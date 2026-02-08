# Design Tokens

Design tokens are centralized, reusable Tailwind CSS class definitions that ensure consistent styling across the
application.

## Overview

Instead of hardcoding Tailwind classes in components, import and use design tokens from `designTokens.ts`. This
provides:

- **Consistency**: Same styling patterns across all components
- **Maintainability**: Change styling in one place, affects all components
- **Scalability**: Easy to theme the application
- **DRY Principle**: Eliminates duplicate class definitions

## Available Tokens

### Container Styles (`containerStyles`)

```typescript
import {containerStyles} from '../../theme/designTokens'

// Default section container with border and background
<div className = {containerStyles.section} >
...
</div>

// Inline form wrapper with border
< div
className = {containerStyles.inlineFormWrapper} >
...
</div>

// Loading state container
< div
className = {containerStyles.loadingContainer} >
...
</div>

// Main spacing container for form display
< div
className = {containerStyles.formContainer} >
...
</div>

// Large spacing container for page layout
< div
className = {containerStyles.pageContainer} >
...
</div>
```

### Button Styles (`buttonStyles`)

```typescript
import {buttonStyles} from '../../theme/designTokens'

// Primary action button (blue)
<button className = {buttonStyles.primaryButton} > Click < /button>

    // Primary button with transition
    < button
className = {buttonStyles.primaryButtonWithTransition} > Click < /button>

    // Secondary button (gray)
    < button
className = {buttonStyles.secondaryButton} > Cancel < /button>

    // Close button (text style)
    < button
className = {buttonStyles.closeButton} >✕</button>
```

### Text Styles (`textStyles`)

```typescript
import {textStyles} from '../../theme/designTokens'

// Section heading with margin
<h3 className = {textStyles.sectionHeading} > Available
Actions < /h3>

// Form display title
< h4
className = {textStyles.formTitle} > Form
Title < /h4>

// Secondary text color
< p
className = {textStyles.secondaryText} > Subtitle < /p>
```

### Layout Styles (`layoutStyles`)

```typescript
import {layoutStyles} from '../../theme/designTokens'

// Flex container for button groups
<div className = {layoutStyles.buttonGroup} >
...
</div>

// Flex container with spacing for form controls
< div
className = {layoutStyles.formControls} >
...
</div>

// Header row with space between items
< div
className = {layoutStyles.headerRow} >
...
</div>

// Content area with vertical spacing
< div
className = {layoutStyles.verticalStack} >
...
</div>
```

### Spinner Styles (`spinnerStyles`)

```typescript
import {spinnerStyles} from '../../theme/designTokens'

// Loading spinner
<div className = {spinnerStyles.spinner} > </div>

    // Loading state text
    < span
className = {spinnerStyles.loadingText} > Loading
...
</span>

// Loading state message color
< div
className = {spinnerStyles.loadingMessage} > Loading
data
...
</div>
```

### Link Section Styles (`linkSectionStyles`)

Used by `HalLinksSection` component:

```typescript
// Links section container
<div className = {linkSectionStyles.container} >
...
</div>

// Links heading
< h3
className = {linkSectionStyles.heading} > Available
Actions < /h3>

// Button container
< div
className = {linkSectionStyles.buttonContainer} >
...
</div>

// Individual link button
< button
className = {linkSectionStyles.linkButton} > Next < /button>
```

### Forms Section Styles (`formsSectionStyles`)

Used by `HalFormsSection` component:

```typescript
// Forms section container
<div className = {formsSectionStyles.container} >
...
</div>

// Forms heading
< h3
className = {formsSectionStyles.heading} > Available
Forms < /h3>

// Button container
< div
className = {formsSectionStyles.buttonContainer} >
...
</div>
```

### Modal Styles (`modalStyles`)

Used by `ModalOverlay` component:

```typescript
// Modal backdrop overlay
<div className = {modalStyles.backdrop} >
...
</div>

// Modal content container
< div
className = {modalStyles.content} >
...
</div>
```

### Error Display Styles (`errorStyles`)

Used by `ErrorDisplay` component:

```typescript
// Error list item
<ul className = {errorStyles.listItem} >
...
</ul>

// Error validation message
< p
className = {errorStyles.validationMessage} >
...
</p>
```

## Adding New Tokens

To add new design tokens:

1. Open `src/theme/designTokens.ts`
2. Create a new object for the category or add to an existing one
3. Document the token with JSDoc comments
4. Export the new token group if needed

Example:

```typescript
export const newStyles = {
    /** Description of this style */
    styleName: 'tailwind-classes here',
}
```

## Updating Existing Tokens

1. Edit the token definition in `designTokens.ts`
2. The change automatically applies to all components using that token
3. No component changes needed (unless the token grouping changes)

Example modification:

```typescript
// Before
primaryButton: 'px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm'

// After
primaryButton: 'px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 text-sm shadow-md'
```

## Best Practices

1. **Use tokens consistently**: Prefer tokens over hardcoded classes
2. **Group related styles**: Keep similar styles in the same object
3. **Document tokens**: Add comments explaining when to use each token
4. **Keep tokens simple**: Don't create a token for everything, only reused patterns
5. **Naming**: Use clear, descriptive names (e.g., `primaryButton`, not `btn1`)

## Migration Guide

If you have hardcoded Tailwind classes that should be tokens:

**Before:**

```typescript
<div className = "mt-4 p-4 border rounded bg-blue-50 dark:bg-blue-900" >
<h3 className = "font-semibold mb-2" > Actions < /h3>
    < /div>
```

**After:**

```typescript
import {linkSectionStyles} from '../../theme/designTokens'

<div className = {linkSectionStyles.container} >
<h3 className = {linkSectionStyles.heading} > Actions < /h3>
    < /div>
```

## Component Usage Examples

### HalLinksSection

```typescript
import {linkSectionStyles} from '../../theme/designTokens'

export function HalLinksSection() {
    return (
        <div className = {linkSectionStyles.container} >
        <h3 className = {linkSectionStyles.heading} > Available
    Links < /h3>
    < div
    className = {linkSectionStyles.buttonContainer} >
    <button className = {linkSectionStyles.linkButton} > Link < /button>
        < /div>
        < /div>
)
}
```

### ErrorDisplay

```typescript
import {buttonStyles, layoutStyles, errorStyles} from '../../theme/designTokens'

export function ErrorDisplay({error, onRetry, onCancel}) {
    return (
        <div className = {layoutStyles.verticalStack} >
            <p>{error.message} < /p>
    {
        error.validationErrors && (
            <ul className = {errorStyles.listItem} >
                {/* validation errors */}
                < /ul>
        )
    }
    <div className = {layoutStyles.formControls} >
    <button className = {buttonStyles.primaryButtonWithTransition} > Retry < /button>
        < button
    className = {buttonStyles.secondaryButton} > Cancel < /button>
        < /div>
        < /div>
)
}
```
