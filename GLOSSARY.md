# Klabis

Club management for an orienteering club: members, events, registrations, fees, and synchronisation with external systems such as ORIS.

## Language

### Events

**Event category**:
A fee level of an event — for example a class a competitor registers for. It has a name, an optional fee and a stable identity that survives a rename, so a registration stays attached to the same category.
_Avoid_: class, event class, price group

**Category identity** (identita kategorie):
The rule that ties a requested category row to an existing one: a row carrying an id means that category renamed and repriced, a row without an id is a new category, and a category of the event that no row names is removed from the event.
_Avoid_: category merge, category matching

**Category change**:
A requested name and fee for a known category, or a new category when no id is named.
_Avoid_: category update DTO, category patch

### Membership fees

**Fee year** (příspěvkový rok):
One year of membership fees: the fee selection campaign for that year, the fee groups published from its levels, and the yearly charge raised once the campaign closes. A member belongs to at most one fee group per fee year.
_Avoid_: campaign year, publication

### Synchronisation

**ORIS event import**:
Bringing events from the ORIS feed into Klabis in one batch, with a per-event result; events Klabis already has are not offered.
_Avoid_: ORIS sync, ORIS event sync, event import

**Sync pairing**:
The link between a Klabis entity and its counterpart in an external system. An entity's pairing is either none, active or retired.
_Avoid_: enrollment, enrolled, sync binding

**Active pairing**:
A sync pairing the system still keeps in step with the external system.
_Avoid_: enrolled, live pairing

**Retired pairing**:
A sync pairing that is no longer kept in step, for example after an event finished or a discipline was archived. Its synchronisation history stays available.
_Avoid_: deleted pairing, unpaired
