package com.klabis.sync;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@DisplayName("DataSyncImpl")
@ExtendWith(MockitoExtension.class)
class DataSyncImplTest {

    private final SyncRecords syncRecords = new FakeSyncRecords();

    @Mock
    private SyncLine<?, ?> syncSource;

    private DataSyncImpl testedInstance = new DataSyncImpl(List.of(syncSource), syncRecords);

}
