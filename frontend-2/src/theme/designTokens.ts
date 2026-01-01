/**
 * Design Tokens for HalNavigator2 and UI Components
 * Centralized Tailwind CSS class definitions for consistent styling
 */

/**
 * Container and Section Styles
 */
export const containerStyles = {
    /** Default section container with border and background */
    section: 'mt-4 p-4 border rounded bg-surface-raised',

    /** Inline form wrapper with border */
    inlineFormWrapper: 'border rounded-lg p-6',

    /** Loading state container */
    loadingContainer: 'flex items-center gap-2 p-4 bg-surface-raised rounded',

    /** Main spacing container for form display */
    formContainer: 'space-y-4',

    /** Large spacing container for page layout */
    pageContainer: 'space-y-6',
}

/**
 * Button and Action Styles
 */
export const buttonStyles = {
    /** Primary action button (blue) */
    primaryButton: 'px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm border-none cursor-pointer',

    /** Primary button with transition */
    primaryButtonWithTransition: 'text-sm px-3 py-1 bg-primary text-white rounded hover:bg-primary-dark transition-colors',

    /** Secondary button (gray) */
    secondaryButton: 'text-sm px-3 py-1 bg-gray-300 text-gray-900 rounded hover:bg-gray-400 transition-colors',

    /** Close button (text style) */
    closeButton: 'text-sm text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-200 transition-colors',
}

/**
 * Heading and Text Styles
 */
export const textStyles = {
    /** Section heading with margin */
    sectionHeading: 'font-semibold mb-2',

    /** Form display title */
    formTitle: 'font-semibold',

    /** Secondary text color */
    secondaryText: 'text-sm text-text-secondary',
}

/**
 * Layout and Spacing Styles
 */
export const layoutStyles = {
    /** Flex container for button groups */
    buttonGroup: 'flex flex-wrap gap-2',

    /** Flex container with spacing for form controls */
    formControls: 'flex gap-2 pt-1',

    /** Header row with space between items */
    headerRow: 'flex items-center justify-between mb-4',

    /** Content area with vertical spacing */
    verticalStack: 'space-y-2',
}

/**
 * Loading and Spinner Styles
 */
export const spinnerStyles = {
    /** Loading spinner */
    spinner: 'animate-spin rounded-full h-5 w-5 border-b-2 border-gray-900 dark:border-gray-100',

    /** Loading state text */
    loadingText: 'text-gray-900 dark:text-gray-100',

    /** Loading state message color */
    loadingMessage: 'text-gray-900 dark:text-gray-100',
}

/**
 * Link Section Styles (HalLinksSection)
 */
export const linkSectionStyles = {
    /** Links section container */
    container: 'mt-4 p-4 border rounded bg-blue-50 dark:bg-blue-900',

    /** Links heading */
    heading: 'font-semibold mb-2',

    /** Button container */
    buttonContainer: 'flex flex-wrap gap-2',

    /** Individual link button */
    linkButton: 'px-3 py-1 bg-blue-600 text-white rounded hover:bg-blue-700 text-sm border-none cursor-pointer',
}

/**
 * Forms Section Styles (HalFormsSection)
 */
export const formsSectionStyles = {
    /** Forms section container */
    container: 'mt-4 p-4 border border-border rounded bg-surface-raised',

    /** Forms heading */
    heading: 'font-semibold mb-2',

    /** Button container */
    buttonContainer: 'flex flex-wrap gap-2',
}

/**
 * Modal Styles
 */
export const modalStyles = {
    /** Modal backdrop overlay - uses pointer-events to allow interaction with content beneath */
    backdrop: 'fixed inset-0 bg-black/40 flex items-center justify-center z-50 animate-in fade-in duration-300 backdrop-blur-md',

    /** Modal content container - with strong shadow and border for visual separation */
    content: 'bg-surface w-full max-h-[90vh] overflow-y-auto p-6 rounded-lg shadow-2xl border border-gray-700/50 animate-in fade-in zoom-in-95 duration-300 relative z-51',
}

/**
 * Error Display Styles
 */
export const errorStyles = {
    /** Error list item */
    listItem: 'list-disc list-inside text-sm',

    /** Error validation message */
    validationMessage: 'text-sm',
}
