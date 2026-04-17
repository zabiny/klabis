package com.klabis.members.traininggroup.infrastructure.bootstrap;

import com.klabis.common.bootstrap.BootstrapDataInitializer;
import com.klabis.members.MemberId;
import com.klabis.members.domain.MemberRepository;
import com.klabis.members.domain.RegistrationNumber;
import com.klabis.groups.common.domain.TrainingGroupFilter;
import com.klabis.members.traininggroup.domain.AgeRange;
import com.klabis.members.traininggroup.domain.TrainingGroup;
import com.klabis.members.traininggroup.domain.TrainingGroupRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
class TrainingGroupDataBootstrap implements BootstrapDataInitializer {

    private static final Logger LOG = LoggerFactory.getLogger(TrainingGroupDataBootstrap.class);

    private final TrainingGroupRepository trainingGroupRepository;
    private final MemberRepository memberRepository;

    TrainingGroupDataBootstrap(TrainingGroupRepository trainingGroupRepository, MemberRepository memberRepository) {
        this.trainingGroupRepository = trainingGroupRepository;
        this.memberRepository = memberRepository;
    }

    @Override
    public boolean requiresBootstrap() {
        return trainingGroupRepository.findAll(TrainingGroupFilter.all()).isEmpty();
    }

    @Override
    public void bootstrapData() {
        MemberId trainerId = memberRepository.findByRegistrationNumber(RegistrationNumber.of("ZBM9000"))
                .map(member -> member.getId())
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
