# Frontend-developer memory

## Project

- [Self-contained registration dialog](project_self_contained_registration_dialog.md) — dialog input je jen Link|null, mode z affordancí přes useRegistrationDialogData, label noRegistrationAffordance, state-reset efekt na pole ne objekt
- [event-category-identity frontend iteration 2](project_event_category_identity_frontend.md) — no separate "Moje přihláška" component, orphaned category display pattern, hideEmptyColumns gotcha, openapi regen against live 8443 backend without touching docs/
- [sync status UI I1+I4](project_sync_status_ui.md) — generic HAL sub-resource indicator (I1) + manager overlay (I4); SYNC:MANAGE gating via _templates; shared SYNC_STATUS_MAP for icon/variant

## Feedback

- [TableCell column key collision](feedback_tablecell_key_collision.md) — two `<TableCell column="_links">` siblings trigger React duplicate-key warning; use unique column id like `_links-sync` since column is just a React key + sort field, dataRender does its own extraction
- [Modal close button scope](feedback_modal_close_button_scope.md) — Modal renders close button OUTSIDE children wrapper; use screen.getByTestId not within(overlay)
- [HalRouteProvider + useRootNavigation test setup](feedback_test_setup_hal_route_provider.md) — 4 wrappers needed (Query/MemoryRouter/HalRoute/HalForm/HalFormsPageLayout) + useRootNavigation mock for overlay-with-HalFormButton components
