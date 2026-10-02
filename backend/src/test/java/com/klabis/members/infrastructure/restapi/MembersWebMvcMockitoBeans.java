package com.klabis.members.infrastructure.restapi;

import com.klabis.members.application.ManagementPort;
import com.klabis.members.application.MemberAccountActivationPort;
import com.klabis.members.application.MemberCompletenessPort;
import com.klabis.members.application.RegistrationPort;
import com.klabis.members.legalguardian.application.GuardianCandidatesPort;
import com.klabis.members.legalguardian.application.LegalGuardianPort;
import com.klabis.members.legalguardiangroup.application.LegalGuardianGroupPort;
import com.klabis.members.LegalGuardians;
import com.klabis.members.Members;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mocks the ports required by the web beans of the members module (member, registration, legal guardian
 * and guardian group controllers, link processors) including the public {@link Members} and
 * {@link LegalGuardians} lookups used by other modules' web beans.
 * <p>
 * {@code MemberDiscoveryPort} is deliberately absent: controllers inject it as {@code Optional}, so it acts
 * as a feature flag and must be mocked explicitly only by tests that want the feature enabled.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@MockitoBean(types = {
        ManagementPort.class,
        RegistrationPort.class,
        MemberCompletenessPort.class,
        MemberAccountActivationPort.class,
        LegalGuardianPort.class,
        GuardianCandidatesPort.class,
        LegalGuardianGroupPort.class,
        LegalGuardians.class,
        Members.class
})
public @interface MembersWebMvcMockitoBeans {
}
