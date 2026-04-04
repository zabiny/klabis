package com.klabis.members.traininggroup.infrastructure.jdbc;

import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("training_group_trainers")
class TrainingGroupTrainerMemento {

    @Column("member_id")
    private UUID memberId;

    protected TrainingGroupTrainerMemento() {
    }

    TrainingGroupTrainerMemento(UUID memberId) {
        this.memberId = memberId;
    }

    UUID getMemberId() {
        return memberId;
    }
}
