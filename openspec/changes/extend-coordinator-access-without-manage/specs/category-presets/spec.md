# Spec Delta

## REMOVED Requirements

### Requirement: Apply Preset to Event

**Reason**: Duplicated the `events` requirement "Apply Category Preset in Event Form". Applying a preset is part of the event form, which is now also available to event coordinators; keeping two copies would force every change to be made twice.

**Migration**: The behaviour, including the rule that a preset is copied into the event without an ongoing link, is specified in `events` › "Apply Category Preset in Event Form". Preset management (the presets page, managers only) stays in this capability unchanged.
