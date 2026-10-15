package com.klabis.members;

import com.klabis.members.application.EffectiveContact;
import com.klabis.members.application.MemberContactPort;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Not a plain {@code @MockitoBean}: member detail dereferences the result, so an unstubbed call must yield an
 * empty contact instead of {@code null}. Tests stub it via {@code @Autowired} like any other mock.
 */
@TestConfiguration(proxyBeanMethods = false)
class MemberContactPortStubConfiguration {

    @Bean
    MemberContactPort memberContactPort() {
        return Mockito.mock(MemberContactPort.class, invocation ->
                invocation.getMethod().getReturnType() == EffectiveContact.class ? EffectiveContact.EMPTY : null);
    }
}
