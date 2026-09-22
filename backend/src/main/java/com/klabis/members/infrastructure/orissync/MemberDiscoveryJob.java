package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.dto.ClubMember;
import com.klabis.common.OrisIntegrationComponent;
import com.klabis.sync.application.SynchronizationPort;
import com.klabis.sync.domain.ExternalReference;
import com.klabis.sync.domain.ExternalSystem;
import com.klabis.sync.domain.SyncEntityType;
import org.jmolecules.architecture.hexagonal.Application;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Discovers ORIS club members Klabis has never seen and enrols them for
 * synchronisation (design.md D7), mirroring {@code DisciplineDiscoveryJob} exactly:
 * read the whole club once, subtract already-paired external ids via
 * {@link SynchronizationPort#findByExternalReferences}, call {@code pullAndEnroll} per
 * new id, catching per-member failures so one bad record cannot stop the run.
 * <p>
 * Two differences from the discipline job (design.md D7):
 * <ul>
 *     <li>It filters candidates on {@link ClubMember#valid()} before enrolling
 *     (design.md D5) — an already-paired member's validity is never even looked at,
 *     since the engine keeps it current regardless of ORIS membership lapsing.</li>
 *     <li>It does nothing at all, logging rather than surfacing a failure, when no
 *     club key is held (design.md D9) — {@link OrisClubMembers#listClubMembers()}
 *     throws {@link ClubKeyNotSetException} before ORIS is ever contacted.</li>
 * </ul>
 * Runs on its own {@code klabis.members.oris-discovery-cron} schedule
 * ({@link MembersOrisDiscoveryProperties}), independent of the sync engine's own
 * {@code klabis.sync.scan-cron}/{@code due-scan-interval}.
 * <p>
 * Classified as {@link Application}, matching {@link MemberSyncAdapter}: this class
 * drives {@link OrisClubMembers} (the ORIS integration's own facade) while also
 * calling {@link SynchronizationPort}, {@code sync}'s primary port.
 * <p>
 * Public (rather than package-private) solely so {@code discoverNewMembers()} can be invoked
 * from {@code MembersApi#importFromOris} (design.md D11, tasks.md section 9): the manual
 * trigger runs the exact same discovery pass the scheduler runs, rather than reimplementing it.
 */
@OrisIntegrationComponent
@Application
public class MemberDiscoveryJob {

    private static final Logger log = LoggerFactory.getLogger(MemberDiscoveryJob.class);

    private final OrisClubMembers orisClubMembers;
    private final SynchronizationPort synchronizationPort;

    public MemberDiscoveryJob(OrisClubMembers orisClubMembers, SynchronizationPort synchronizationPort) {
        this.orisClubMembers = orisClubMembers;
        this.synchronizationPort = synchronizationPort;
    }

    @Scheduled(initialDelayString = "PT10S")
    void onStartup() {
        discoverNewMembers();
    }

    @Scheduled(cron = "${klabis.members.oris-discovery-cron}")
    public void discoverNewMembers() {
        log.info("Starting ORIS member discovery");

        Map<String, ClubMember> clubMembers;
        try {
            clubMembers = orisClubMembers.listClubMembers();
        } catch (ClubKeyNotSetException e) {
            log.info("ORIS member discovery skipped: no ORIS club key is held");
            return;
        }

        List<String> validExternalIds = clubMembers.values().stream()
                .filter(MemberDiscoveryJob::isValid)
                .map(clubMember -> String.valueOf(clubMember.id()))
                .toList();
        if (validExternalIds.isEmpty()) {
            log.info("ORIS member discovery completed: ORIS reported no members with active club membership");
            return;
        }

        Set<String> alreadyPaired = synchronizationPort
                .findByExternalReferences(SyncEntityType.MEMBER, ExternalSystem.ORIS, validExternalIds)
                .stream()
                .map(reference -> reference.externalReference().externalId())
                .collect(Collectors.toSet());

        int discovered = 0;
        for (String externalId : validExternalIds) {
            if (alreadyPaired.contains(externalId)) {
                continue;
            }
            try {
                synchronizationPort.pullAndEnroll(
                        SyncEntityType.MEMBER, new ExternalReference(ExternalSystem.ORIS, externalId), null);
                discovered++;
            } catch (RuntimeException e) {
                log.error("Failed to discover ORIS member {}: {}", externalId, e.getMessage(), e);
            }
        }

        log.info("ORIS member discovery completed: {} new member(s) discovered", discovered);
    }

    private static boolean isValid(ClubMember clubMember) {
        return Boolean.TRUE.equals(clubMember.valid());
    }
}
