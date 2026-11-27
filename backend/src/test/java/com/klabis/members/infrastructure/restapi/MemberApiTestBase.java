package com.klabis.members.infrastructure.restapi;

import com.klabis.members.application.ManagementPort;
import com.klabis.sync.application.SyncPairingLookup;
import com.klabis.sync.domain.SyncPairing;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

abstract class MemberApiTestBase {

    static final String ADMIN_USERNAME = "ZBM0001";
    static final String MEMBER_USERNAME = "ZBM0101";

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ManagementPort managementService;

    @Autowired
    SyncPairingLookup pairingLookup;

    @BeforeEach
    void stubPairingNoneByDefault() {
        when(pairingLookup.pairingOf(any())).thenReturn(SyncPairing.NONE);
    }
}
