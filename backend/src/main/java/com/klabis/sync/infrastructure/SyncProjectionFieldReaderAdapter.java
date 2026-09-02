package com.klabis.sync.infrastructure;

import com.klabis.sync.domain.SyncProjection;
import com.klabis.sync.domain.SyncProjectionFieldReader;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
class SyncProjectionFieldReaderAdapter implements SyncProjectionFieldReader {

    @Override
    public Map<String, Object> fields(SyncProjection projection) {
        return SyncProjectionCodec.toFieldMap(projection);
    }
}
