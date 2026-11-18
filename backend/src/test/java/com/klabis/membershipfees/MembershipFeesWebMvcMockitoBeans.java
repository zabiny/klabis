package com.klabis.membershipfees;

import com.klabis.membershipfees.application.FeeSelectionCampaignManagementPort;
import com.klabis.membershipfees.application.FeeYearLifecyclePort;
import com.klabis.membershipfees.application.MemberFeeHistoryPort;
import com.klabis.membershipfees.application.MembershipFeeTypeManagementPort;
import com.klabis.membershipfees.application.MembershipFeeTypeOptionsPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mocks the ports required by the web beans of the membershipfees module (type, campaign, fee group,
 * fee choice and fee summary controllers, link processors) so that other modules' {@code @WebMvcTest}
 * slices can load them without the membershipfees application layer.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@MockitoBean(types = {
        MembershipFeeTypeManagementPort.class,
        MembershipFeeTypeOptionsPort.class,
        FeeSelectionCampaignManagementPort.class,
        FeeYearLifecyclePort.class,
        MemberFeeHistoryPort.class
})
public @interface MembershipFeesWebMvcMockitoBeans {
}
