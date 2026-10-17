package com.klabis.sync.infrastructure.restapi;

import com.klabis.sync.application.AttemptView;

import java.util.Optional;

record DescribedAttempt(AttemptView view, Optional<EntityDescription> description) {
}
