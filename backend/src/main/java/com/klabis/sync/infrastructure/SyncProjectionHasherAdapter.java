package com.klabis.sync.infrastructure;

import com.klabis.sync.domain.SyncHash;
import com.klabis.sync.domain.SyncProjection;
import com.klabis.sync.domain.SyncProjectionHasher;
import org.springframework.stereotype.Component;

@Component
class SyncProjectionHasherAdapter implements SyncProjectionHasher {

    @Override
    public SyncHash hash(SyncProjection projection) {
        return SyncProjectionCodec.hash(projection);
    }
}
