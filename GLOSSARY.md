# Klabis

Club management for an orienteering club: members, events, registrations, fees, and synchronisation with external systems such as ORIS.

## Language

### Synchronisation

**Sync pairing**:
The link between a Klabis entity and its counterpart in an external system. An entity's pairing is either none, active or retired.
_Avoid_: enrollment, enrolled, sync binding

**Active pairing**:
A sync pairing the system still keeps in step with the external system.
_Avoid_: enrolled, live pairing

**Retired pairing**:
A sync pairing that is no longer kept in step, for example after an event finished or a discipline was archived. Its synchronisation history stays available.
_Avoid_: deleted pairing, unpaired
