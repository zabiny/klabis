## MODIFIED Requirements

### Requirement: Member Detail Page Layout

The member detail page SHALL use a two-column layout driven by the available PATCH template fields, without client-side role detection.

#### Scenario: Detail page with full template (admin or self)

- **WHEN** member detail response includes a PATCH template
- **THEN** the page renders in a two-column layout: left column (personal data, contact, address), right column (supplementary info, documents and licenses)
- **AND** an "Upravit profil" button is shown

#### Scenario: Detail page without template (other member)

- **WHEN** member detail response has no PATCH template
- **THEN** only contact section and address section are displayed
- **AND** no action buttons are shown

#### Scenario: Admin detail shows action buttons with icons

- **WHEN** member detail response includes a full PATCH template (admin view)
- **THEN** the page header shows action buttons: "Upravit profil" (pencil icon), "Vložit / Vybrat" (banknote icon), "Oprávnění" (shield icon, visible only if permissions link present), "Ukončit členství" (user-x icon, red)

#### Scenario: Own profile shows membership and edit buttons

- **WHEN** member detail response includes a self-edit PATCH template (own profile view)
- **THEN** the page header shows: "Členské příspěvky" button and "Upravit profil" button
- **AND** "Oprávnění" and "Ukončit členství" buttons are NOT shown

#### Scenario: Group navigation buttons shown when member belongs to a group

- **WHEN** the member detail response carries a `trainingGroup` HAL link
- **THEN** the page header additionally shows a "Tréninková skupina" button (dumbbell icon, secondary style)
- **AND** clicking it navigates the user to the linked training group detail page
- **WHEN** the member detail response carries a `familyGroup` HAL link
- **THEN** the page header additionally shows a "Rodina" button (heart icon, secondary style)
- **AND** clicking it navigates the user to the linked family group detail page
- **WHEN** the member detail response carries neither link
- **THEN** neither group navigation button is shown
- **AND** this rule applies uniformly to every view of the member detail page (admin view, self-profile view, any other variant)
