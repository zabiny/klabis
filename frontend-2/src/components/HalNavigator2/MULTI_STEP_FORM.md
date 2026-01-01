# MultiStepFormModal Component

A reusable component for creating multi-step modal forms with HAL Forms integration.

## Overview

The `MultiStepFormModal` component simplifies the creation of multi-step forms by providing:

- **Step-by-step navigation** with validation between steps
- **Progress indicator** showing current step
- **Error handling** with automatic reset to step 1 on submission failure
- **Field rendering** via HAL Forms context
- **Flexible configuration** with step definitions

## Usage

### Basic Example

```tsx
import {MultiStepFormModal, type FormStep} from '../components/HalNavigator2/MultiStepFormModal';
import {HalFormButton} from '../components/HalNavigator2/HalFormButton';

const steps: FormStep[] = [
    {
        title: 'Step 1: Personal Info',
        fields: ['firstName', 'lastName', 'email'],
    },
    {
        title: 'Step 2: Address',
        fields: ['street', 'city', 'postalCode'],
    },
    {
        title: 'Step 3: Additional',
        fields: ['phone', 'notes'],
    },
];

export const MyForm = () => {
    return (
        <HalFormButton
            name="createItem"
            customLayout={<MultiStepFormModal steps={steps} />}
        />
    );
};
```

### Member Registration Example (from MembersPage)

```tsx
const memberRegistrationSteps: FormStep[] = [
    {
        title: 'Krok 1: Osobní údaje',
        fields: ['firstName', 'lastName', 'sex', 'dateOfBirth', 'birthCertificateNumber', 'nationality'],
    },
    {
        title: 'Krok 2: Kontaktní informace',
        fields: ['address', 'contact', 'guardians'],
    },
    {
        title: 'Krok 3: Údaje člena',
        fields: ['siCard', 'bankAccount', 'registrationNumber', 'orisId'],
    },
];

const RegisterMemberFormButton = () => {
    return (
        <HalFormButton
            name="memberRegistrationsPost"
            customLayout={<MultiStepFormModal steps={memberRegistrationSteps} />}
        />
    );
};
```

## Props

### `steps` (required)

- **Type:** `FormStep[]`
- **Description:** Array of step definitions

### `nextButtonLabel`

- **Type:** `string`
- **Default:** `"Další"`
- **Description:** Text for the next button

### `backButtonLabel`

- **Type:** `string`
- **Default:** `"Zpět"`
- **Description:** Text for the back button

### `submitButtonLabel`

- **Type:** `string`
- **Default:** `"Odeslat"`
- **Description:** Text for the submit button

### `cancelButtonLabel`

- **Type:** `string`
- **Default:** `"Zrušit"`
- **Description:** Text for the cancel button (rendered by HAL Forms)

### `showStepNumbers`

- **Type:** `boolean`
- **Default:** `true`
- **Description:** Whether to show step numbers in the title (e.g., "(1/3)")

## FormStep Interface

```typescript
interface FormStep {
    /** Display title for this step */
    title: string;
    /** Field names to render in this step */
    fields: string[];
}
```

## How It Works

### Step Navigation

1. User fills in fields for the current step
2. Clicking "Next" validates all fields in current step
3. If valid, moves to next step
4. If invalid, displays validation errors and stays on current step
5. User can click "Back" to return to previous steps without validation

### Form Submission

1. User completes all steps and reaches the final step
2. On final step, "Next" button is replaced with "Submit" button
3. Clicking "Submit" validates final step and submits the form
4. A progress bar shows which step user is on

### Error Handling

- If API returns validation errors (HTTP 400), form automatically resets to step 1
- User can review values and navigate through steps again
- User can fix errors and resubmit from the final step

## Validation Flow

```
Step 1 Fields Validation
    ↓
[Next] → Validate Step 1 Fields
    ↓
All Valid? → Advance to Step 2
Not Valid? → Show Errors, Stay on Step 1
    ↓
Step 2 Fields Validation
    ↓
[Next] → Validate Step 2 Fields
    ↓
All Valid? → Advance to Step 3
Not Valid? → Show Errors, Stay on Step 2
    ↓
Step 3 Fields Validation
    ↓
[Submit] → Validate Step 3 Fields
    ↓
All Valid? → Submit Form
Not Valid? → Show Errors, Stay on Step 3
    ↓
Backend Validation
    ↓
Success? → Close Form
Failure? → Reset to Step 1, Show Errors
```

## Styling

The component uses Tailwind CSS classes:

- Progress bar: `flex gap-2` with `flex-1 h-1 rounded` segments
- Buttons: Standard Tailwind `Button` component variants
- Spacing: `space-y-6`, `space-y-4`, `gap-3` for consistent spacing
- Border: `pt-4 border-t border-gray-200` separates actions

## Requirements

- Must be used within `HalFormsForm` component
- Requires Formik context from `useFormikContext()`
- Requires HAL Forms context from `HalFormsFormContext`

## Integration Points

### With HalFormButton

```tsx
<HalFormButton
    name="formTemplateName"
    modal={true}
    customLayout={<MultiStepFormModal steps={steps}/>}
/>
```

### With Custom Label Text

```tsx
<MultiStepFormModal
    steps={steps}
    nextButtonLabel="→ Pokračovat"
    backButtonLabel="← Zpět"
    submitButtonLabel="Uložit"
    showStepNumbers={true}
/>
```

## Advanced Examples

### Conditional Field Display

```tsx
const steps: FormStep[] = [
    {
        title: 'Step 1: Basic Info',
        fields: ['name', 'email'],
    },
    {
        title: 'Step 2: Address',
        // Only show address, city (country is optional)
        fields: ['address', 'city'],
    },
];
```

### Large Forms (Many Steps)

```tsx
const steps: FormStep[] = [
    {
        title: 'Step 1: Personal',
        fields: ['firstName', 'lastName', 'dateOfBirth'],
    },
    {
        title: 'Step 2: Contact',
        fields: ['email', 'phone', 'address'],
    },
    {
        title: 'Step 3: Company',
        fields: ['companyName', 'jobTitle', 'department'],
    },
    {
        title: 'Step 4: Preferences',
        fields: ['newsletter', 'notifications', 'marketing'],
    },
    {
        title: 'Step 5: Review',
        fields: ['termsAccepted', 'privacyAccepted'],
    },
];
```

## Error Recovery Scenario

1. User fills steps 1 → 2 → 3
2. Clicks "Submit"
3. Backend returns validation error for field `email` in step 2
4. Form automatically resets to step 1
5. User sees all previously entered values preserved
6. User clicks "Next" to reach step 2
7. User sees validation error on `email` field
8. User corrects the value
9. User navigates through steps again and resubmits

This provides a smooth experience for complex multi-step forms.
