/**
 * Centralized messages for validation and UI
 * Czech locale messages for forms, errors, and user-facing text
 */

export const VALIDATION_MESSAGES = {
	REQUIRED_FIELD: 'Povinné pole',
	INVALID_EMAIL: 'Neplatný email',
	MUST_BE_NUMBER: 'Musí být číslo',
	INVALID_FORMAT: 'Nespravny format',
} as const;

export const UI_MESSAGES = {
	FORM: 'Formulář',
	CLOSE: 'Zavřít',
	SHOW_RAW_JSON: 'Zobrazit zdrojový JSON',
	COMPLETE_JSON: 'Úplná JSON data položky',
	AVAILABLE_ACTIONS: 'Dostupné odkazy',
	AVAILABLE_FORMS: 'Dostupné formuláře',
	COLLECTION_EMPTY: 'Kolekce je prázdná',
	VIEW_FULL_JSON: 'View full JSON',
	SUBMIT: 'Odeslat',
	SUBMITTING: 'Odesílám...',
	LOADING_MENU: 'Loading menu...',
	NO_MENU_ITEMS: 'No menu items available',
	FAILED_LOAD_MENU: 'Failed to load menu',
	FORM_VALIDATION_ERRORS: 'Form validation errors',
} as const;

export const TABLE_HEADERS = {
	ID: 'ID',
	DATA: 'Údaje',
	ACTIONS: 'Akce',
	ATTRIBUTE: 'Atribut',
	VALUE: 'Hodnota',
	ITEMS: 'Položky',
	DETAILS: 'Detaily',
} as const;
