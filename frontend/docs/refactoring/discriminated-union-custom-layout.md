ě# Discriminated Union Refactor for Custom Layout Pattern

## Current Implementation

### Problem Analysis

The current `customLayout` prop uses a union type with runtime type checking:

```typescript
// Current type definition
customLayout ? : ReactNode | RenderFormCallback;

// Runtime type checking in HalFormDisplay.tsx:43-52
function getHalFormsFormProps(customLayout?: ReactNode | RenderFormCallback) {
    if (!customLayout) {
        return {};
    }

    if (typeof customLayout === 'function') {
        return {renderForm: customLayout as RenderFormCallback};
    }

    return {children: customLayout as ReactNode};
}
```

### Issues with Current Approach

1. **Type Ambiguity**: `ReactNode` can include function components, which could potentially conflict with the
   `typeof customLayout === 'function'` check

   ```typescript
   // This is a ReactNode (function component)
   const MyComponent = () => <div>Hello</div>;

   // But typeof MyComponent === 'function' is also true!
   // Could be confused with RenderFormCallback
   ```

2. **Runtime Type Assertions**: Uses `as` type assertions which bypass TypeScript's type safety

3. **Implicit Behavior**: Not immediately clear from the API which pattern is being used

4. **Limited Type Safety**: TypeScript can't help catch mistakes like passing the wrong type

## Proposed Discriminated Union Pattern

### Type Definitions

```typescript
/**
 * Children-based layout pattern using HalFormsFormField components
 */
type ChildrenLayout = {
    /** Discriminator for type narrowing */
    type: 'children';
    /** ReactNode layout with HalFormsFormField components */
    layout: ReactNode;
};

/**
 * Callback-based layout pattern using renderField function
 */
type CallbackLayout = {
    /** Discriminator for type narrowing */
    type: 'callback';
    /** Callback function that receives renderField */
    layout: RenderFormCallback;
};

/**
 * Discriminated union for custom layouts
 */
type CustomLayout = ChildrenLayout | CallbackLayout;

// Updated component props
interface HalFormDisplayProps {
    // ... other props
    customLayout?: CustomLayout;
}
```

### Implementation

```typescript
/**
 * Builds props for HalFormsForm based on customLayout discriminator
 */
function getHalFormsFormProps(customLayout?: CustomLayout): Record<string, ReactNode | RenderFormCallback> {
    if (!customLayout) {
        return {};
    }

    // Type-safe switch with discriminator
    switch (customLayout.type) {
        case 'callback':
            // TypeScript knows customLayout.layout is RenderFormCallback
            return {renderForm: customLayout.layout};
        case 'children':
            // TypeScript knows customLayout.layout is ReactNode
            return {children: customLayout.layout};
        default:
            // Exhaustiveness check - TypeScript will error if new type added
            const _exhaustive: never = customLayout;
            return {};
    }
}
```

### Usage Examples

#### Before (Current API)

```tsx
// Children pattern - implicit
<HalFormButton
    name="edit"
    customLayout={
        <div>
            <HalFormsFormField fieldName="name"/>
        </div>
    }
/>

// Callback pattern - implicit
<HalFormButton
    name="edit"
    customLayout={(renderField) => (
        <div>{renderField('name')}</div>
    )}
/>
```

#### After (Discriminated Union API)

```tsx
// Children pattern - explicit
<HalFormButton
    name="edit"
    customLayout={{
        type: 'children',
        layout: (
            <div>
                <HalFormsFormField fieldName="name"/>
            </div>
        )
    }}
/>

// Callback pattern - explicit
<HalFormButton
    name="edit"
    customLayout={{
        type: 'callback',
        layout: (renderField) => (
            <div>{renderField('name')}</div>
        )
    }}
/>
```

### Helper Functions (Optional DX Improvement)

To reduce verbosity, provide helper functions:

```typescript
/**
 * Helper to create children-based custom layout
 */
export function childrenLayout(layout: ReactNode): ChildrenLayout {
    return {type: 'children', layout};
}

/**
 * Helper to create callback-based custom layout
 */
export function callbackLayout(layout: RenderFormCallback): CallbackLayout {
    return {type: 'callback', layout};
}
```

Usage with helpers:

```tsx
import {childrenLayout, callbackLayout} from '../components/HalNavigator2/layoutHelpers';

// Children pattern with helper
<HalFormButton
    name="edit"
    customLayout={childrenLayout(
        <div>
            <HalFormsFormField fieldName="name"/>
        </div>
    )}
/>

// Callback pattern with helper
<HalFormButton
    name="edit"
    customLayout={callbackLayout((renderField) => (
        <div>{renderField('name')}</div>
    ))}
/>
```

## Benefits

### 1. **Type Safety**

```typescript
// TypeScript catches this error
const layout: CustomLayout = {
    type: 'callback',
    layout: <div>Wrong! < /div>  /
/ ❌ Type error: ReactNode not assignable to RenderFormCallback
}
;

// Current API can't catch this at compile time
const current: ReactNode | RenderFormCallback = <div>Ambiguous < /div>; /
/ ✅ No error
```

### 2. **Explicit Intent**

```typescript
// Clear which pattern is being used
customLayout = {
{
    type: 'children', layout
:
    <div / >
}
}  // Clearly children
customLayout = {
{
    type: 'callback', layout
:
    (rf) => <div / >
}
}  // Clearly callback
```

### 3. **Better Autocomplete**

```typescript
// When typing customLayout={{type: '
// IDE suggests: 'children' | 'callback'

// After selecting type: 'children'
// IDE knows layout must be ReactNode

// After selecting type: 'callback'
// IDE knows layout must be RenderFormCallback
```

### 4. **Exhaustiveness Checking**

```typescript
// If you add a new layout type:
type TemplateLayout = {
    type: 'template';
    templateId: string;
};

type CustomLayout = ChildrenLayout | CallbackLayout | TemplateLayout;

// TypeScript will error on the switch statement until you handle 'template' case
// This prevents bugs from forgetting to handle new cases
```

### 5. **No Runtime Type Checking**

```typescript
// Current: Runtime check with potential edge cases
if (typeof customLayout === 'function') { /* ... */
}

// Discriminated union: Compile-time guarantee
switch (customLayout.type) { /* ... */
}
```

### 6. **Easier Testing**

```typescript
// Current: Need to test runtime type detection
it('should detect callback pattern', () => {
    const callback = (rf) => <div / >;
    expect(typeof callback === 'function').toBe(true);
});

// Discriminated union: Type is explicit
it('should handle callback pattern', () => {
    const layout: CallbackLayout = {type: 'callback', layout: (rf) => <div / >};
    expect(layout.type).toBe('callback');  // No ambiguity
});
```

## Drawbacks

### 1. **More Verbose API**

```typescript
// Current: Simple
customLayout = { < div / >
}

// Discriminated: Verbose
customLayout = {
{
    type: 'children', layout
:
    <div / >
}
}

// Mitigated by helpers:
customLayout = {childrenLayout( < div / >
)
}
```

### 2. **Breaking Change**

- Would require migration for all existing code
- Need to update all components: `HalFormButton`, `HalFormDisplay`, `HalFormsPageLayout`, `HalFormsSection`
- Need to update all documentation and examples

### 3. **Learning Curve**

- Developers need to understand discriminated unions
- Need to remember to use correct type discriminator
- More concepts to explain in documentation

### 4. **Migration Effort**

```typescript
// Need to find and replace all usages:
// Before
<HalFormButton customLayout = {(rf)
=>
<div / >
}
/>

// After
< HalFormButton
customLayout = {
{
    type: 'callback', layout
:
    (rf) => <div / >
}
}
/>
```

## Migration Strategy

### Phase 1: Add Discriminated Union Support (Non-Breaking)

Support both old and new API during transition:

```typescript
type LegacyCustomLayout = ReactNode | RenderFormCallback;
type CustomLayout = ChildrenLayout | CallbackLayout;

interface HalFormDisplayProps {
    customLayout?: CustomLayout | LegacyCustomLayout;
}

function getHalFormsFormProps(customLayout?: CustomLayout | LegacyCustomLayout) {
    if (!customLayout) {
        return {};
    }

    // Check if discriminated union
    if (typeof customLayout === 'object' && 'type' in customLayout) {
        switch (customLayout.type) {
            case 'callback':
                return {renderForm: customLayout.layout};
            case 'children':
                return {children: customLayout.layout};
        }
    }

    // Legacy support - runtime type check
    if (typeof customLayout === 'function') {
        console.warn('Passing function directly to customLayout is deprecated. Use {type: "callback", layout: fn}');
        return {renderForm: customLayout as RenderFormCallback};
    }

    console.warn('Passing ReactNode directly to customLayout is deprecated. Use {type: "children", layout: node}');
    return {children: customLayout as ReactNode};
}
```

### Phase 2: Deprecation Warnings

Add deprecation warnings to encourage migration:

```typescript
/**
 * @deprecated Use {type: 'callback', layout: fn} instead
 */
type DeprecatedCallbackLayout = RenderFormCallback;

/**
 * @deprecated Use {type: 'children', layout: node} instead
 */
type DeprecatedChildrenLayout = ReactNode;
```

### Phase 3: Remove Legacy Support (Breaking Change)

After sufficient transition period, remove legacy support and update to discriminated union only.

## Recommendation

### For New Projects

✅ **Use discriminated union from the start** - Better type safety and clearer API outweigh verbosity

### For Existing Projects (like klabis)

⚠️ **Keep current implementation** unless:

- You're doing a major version bump
- You have many custom layout usages with bugs from type ambiguity
- Your team values type safety over API simplicity

### Middle Ground

Consider the **helper functions approach**:

- Add `childrenLayout()` and `callbackLayout()` helpers alongside current API
- Encourage new code to use helpers for clarity
- Keep current API for backward compatibility
- Document both patterns

```typescript
// Both work - developers choose preference:
customLayout = {(rf)
=>
<div / >
}  // Short but implicit
customLayout = {callbackLayout((rf)
=>
<div / >
)
}  // Verbose but explicit
```

## Conclusion

The discriminated union pattern provides:

- ✅ Better type safety
- ✅ Clearer intent
- ✅ Future extensibility
- ❌ More verbose API
- ❌ Breaking change

**Recommendation for klabis**: Keep current implementation, but document this pattern for future consideration in major
version updates. The current implementation works well and the verbosity cost outweighs the type safety benefits for
this specific use case.

If type ambiguity becomes a real issue in production, revisit this refactor.
