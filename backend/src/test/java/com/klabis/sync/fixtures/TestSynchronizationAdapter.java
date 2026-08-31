package com.klabis.sync.fixtures;

import com.klabis.sync.domain.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Test double for {@link SynchronizationAdapter} with configurable capabilities and
 * controllable state on both sides — the harness every slice's integration tests
 * extend (tasks.md 1.14). Not a Spring bean: constructed directly by tests so each
 * test controls exactly which capabilities and starting state it needs.
 */
public class TestSynchronizationAdapter implements SynchronizationAdapter {

    private final SyncEntityType entityType;
    private final ExternalSystem system;
    private SyncCapabilities capabilities;
    private final Map<String, TestSyncProjection> localState = new HashMap<>();
    private final Map<String, TestSyncProjection> externalState = new HashMap<>();
    private ExternalVersionToken versionToken;
    private int externalReadCount = 0;
    private int localReadCount = 0;

    public TestSynchronizationAdapter(SyncEntityType entityType, ExternalSystem system) {
        this.entityType = entityType;
        this.system = system;
        this.capabilities = new SyncCapabilities(true, true, true, false, false, false, false);
    }

    public TestSynchronizationAdapter withCapabilities(SyncCapabilities capabilities) {
        this.capabilities = capabilities;
        return this;
    }

    public TestSynchronizationAdapter withLocalState(String entityId, TestSyncProjection projection) {
        localState.put(entityId, projection);
        return this;
    }

    public TestSynchronizationAdapter withExternalState(String externalId, TestSyncProjection projection) {
        externalState.put(externalId, projection);
        return this;
    }

    public TestSynchronizationAdapter withVersionToken(ExternalVersionToken token) {
        this.versionToken = token;
        return this;
    }

    /**
     * Clears the version token and read counters. This adapter is typically wired as
     * a Spring singleton bean shared across every test in a class, so a test that sets
     * a version token or relies on read counts should reset it in {@code @BeforeEach}
     * to avoid leaking state into unrelated tests.
     */
    public void reset() {
        this.versionToken = null;
        this.externalReadCount = 0;
        this.localReadCount = 0;
    }

    public int externalReadCount() {
        return externalReadCount;
    }

    public int localReadCount() {
        return localReadCount;
    }

    @Override
    public SyncEntityType entityType() {
        return entityType;
    }

    @Override
    public ExternalSystem system() {
        return system;
    }

    @Override
    public SyncCapabilities capabilities() {
        return capabilities;
    }

    @Override
    public Class<? extends SyncProjection> projectionType() {
        return TestSyncProjection.class;
    }

    @Override
    public SyncProjection readLocal(String entityId) {
        localReadCount++;
        TestSyncProjection projection = localState.get(entityId);
        if (projection == null) {
            throw new IllegalStateException("No local test state configured for entityId " + entityId);
        }
        return projection;
    }

    @Override
    public SyncProjection readExternal(String externalId) {
        externalReadCount++;
        TestSyncProjection projection = externalState.get(externalId);
        if (projection == null) {
            throw new IllegalStateException("No external test state configured for externalId " + externalId);
        }
        return projection;
    }

    @Override
    public Optional<ExternalVersionToken> externalVersion(String externalId) {
        return Optional.ofNullable(versionToken);
    }

    @Override
    public void applyToLocal(String entityId, SyncProjection projection) {
        localState.put(entityId, (TestSyncProjection) projection);
    }

    @Override
    public void applyToExternal(String externalId, SyncProjection projection) {
        if (!capabilities.writesExternal()) {
            throw new UnsupportedOperationException("This test adapter does not declare an outward write capability");
        }
        externalState.put(externalId, (TestSyncProjection) projection);
    }
}
