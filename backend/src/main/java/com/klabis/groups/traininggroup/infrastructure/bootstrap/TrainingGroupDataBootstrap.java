package com.klabis.groups.traininggroup.infrastructure.bootstrap;

import com.klabis.common.bootstrap.BootstrapDataInitializer;
import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.groups.traininggroup.domain.AgeRange;
import com.klabis.groups.traininggroup.domain.TrainingGroup;
import com.klabis.groups.traininggroup.domain.TrainingGroupRepository;
import com.klabis.members.MemberId;
import com.klabis.members.Members;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class TrainingGroupDataBootstrap implements BootstrapDataInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(TrainingGroupDataBootstrap.class);

    private final TrainingGroupRepository trainingGroupRepository;
    private final Members members;

    TrainingGroupDataBootstrap(TrainingGroupRepository trainingGroupRepository, Members members) {
        this.trainingGroupRepository = trainingGroupRepository;
        this.members = members;
    }

    @Override
    public boolean requiresBootstrap() {
        return trainingGroupRepository.findAll(TrainingGroupFilter.all()).isEmpty();
    }

    @Override
    public void bootstrapData() {
        MemberId trainerId = members.findByRegistrationNumber("ZBM9000")
                .map(m -> new MemberId(m.memberId()))
                .orElse(null);

        if (trainerId == null) {
            LOG.warn("Bootstrap trainer (ZBM9000) not found — skipping training group bootstrap");
            return;
        }

        trainingGroupRepository.save(TrainingGroup.create(new TrainingGroup.CreateTrainingGroup("Pulci", trainerId, new AgeRange(8, 9))));
        trainingGroupRepository.save(TrainingGroup.create(new TrainingGroup.CreateTrainingGroup("Žáci", trainerId, new AgeRange(10, 14))));
        trainingGroupRepository.save(TrainingGroup.create(new TrainingGroup.CreateTrainingGroup("Dorost", trainerId, new AgeRange(15, 17))));
        trainingGroupRepository.save(TrainingGroup.create(new TrainingGroup.CreateTrainingGroup("Hobby", trainerId, new AgeRange(35, 80))));

        LOG.info("Created 4 bootstrap training groups");
    }
}
