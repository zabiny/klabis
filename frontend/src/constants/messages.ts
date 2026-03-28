/**
 * Backward-compatible re-exports from the central localization system.
 * New code should import directly from '@/localization'.
 */
import { labels } from '../localization';

export const VALIDATION_MESSAGES = {
    REQUIRED_FIELD: labels.validation.requiredField,
    INVALID_EMAIL: labels.validation.invalidEmail,
    MUST_BE_NUMBER: labels.validation.mustBeNumber,
    INVALID_FORMAT: labels.validation.invalidFormat,
} as const;

export const UI_MESSAGES = {
    FORM: labels.ui.form,
    CLOSE: labels.buttons.close,
    SHOW_RAW_JSON: labels.ui.showRawJson,
    COMPLETE_JSON: labels.ui.completeJson,
    AVAILABLE_ACTIONS: labels.ui.availableActions,
    AVAILABLE_FORMS: labels.ui.availableForms,
    COLLECTION_EMPTY: labels.ui.collectionEmpty,
    VIEW_FULL_JSON: labels.ui.viewFullJson,
    SUBMIT: labels.buttons.submit,
    SUBMITTING: labels.buttons.submitting,
    LOADING_MENU: labels.errors.loadingMenu,
    NO_MENU_ITEMS: labels.errors.noMenuItems,
    FAILED_LOAD_MENU: labels.errors.failedLoadMenu,
    FORM_VALIDATION_ERRORS: labels.errors.formValidationErrors,
    LOADING_FORM_DATA: labels.ui.loadingFormData,
    FORM_DATA_LOAD_ERROR: labels.errors.formDataLoadError,
    RETRY: labels.buttons.retry,
    CANCEL: labels.buttons.cancel,
} as const;

export const TABLE_HEADERS = {
    ID: labels.tables.id,
    DATA: labels.tables.data,
    ACTIONS: labels.tables.actions,
    ATTRIBUTE: labels.tables.attribute,
    VALUE: labels.tables.value,
    ITEMS: labels.tables.items,
    DETAILS: labels.tables.details,
} as const;
