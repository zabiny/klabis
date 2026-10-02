package com.klabis.sync.application;

import com.klabis.sync.domain.SyncProjection;
import com.klabis.sync.domain.SyncProjectionFieldReader;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.Map;

@Service
class SyncProjectionFieldsService implements SyncProjectionFieldsPort {

    private final SyncProjectionFieldReader fieldReader;

    SyncProjectionFieldsService(SyncProjectionFieldReader fieldReader) {
        this.fieldReader = fieldReader;
    }

    @Override
    public Map<String, Object> fields(SyncProjection projection) {
        Assert.notNull(projection, "projection is required");
        return fieldReader.fields(projection);
    }
}
