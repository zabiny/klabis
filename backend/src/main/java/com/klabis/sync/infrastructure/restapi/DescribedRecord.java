package com.klabis.sync.infrastructure.restapi;

import com.klabis.sync.application.RecordView;

import java.util.Optional;

record DescribedRecord(RecordView view, Optional<EntityDescription> description) {
}
