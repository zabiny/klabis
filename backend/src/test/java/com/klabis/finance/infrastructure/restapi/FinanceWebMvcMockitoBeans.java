package com.klabis.finance.infrastructure.restapi;

import com.klabis.finance.application.DepositPort;
import com.klabis.finance.application.FinanceAccountLinkSupport;
import com.klabis.finance.application.ChargePort;
import com.klabis.finance.application.ReversePort;
import com.klabis.finance.application.TransactionQueryPort;
import com.klabis.finance.domain.MemberAccountRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mocks the ports required by the web beans of the finance module (member account controller) and
 * {@link FinanceAccountLinkSupport}, which other modules' web beans use to link to member accounts.
 * Mocking the latter overrides the real implementation when finance web beans are loaded as well.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@MockitoBean(types = {
        DepositPort.class,
        ChargePort.class,
        ReversePort.class,
        MemberAccountRepository.class,
        TransactionQueryPort.class,
        FinanceAccountLinkSupport.class
})
public @interface FinanceWebMvcMockitoBeans {
}
