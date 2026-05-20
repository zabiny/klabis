package com.klabis.calendar.infrastructure.security;

import com.klabis.calendar.application.IcalTokenPort;
import com.klabis.common.users.UserId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("IcalTokenAuthenticationFilter")
@ExtendWith(MockitoExtension.class)
class IcalTokenAuthenticationFilterTest {

    @Mock
    private IcalTokenPort icalTokenPort;

    @Mock
    private FilterChain filterChain;

    private IcalTokenAuthenticationFilter filter;

    private static final UserId USER_ID = new UserId(UUID.randomUUID());

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        IcalTokenAuthenticationProvider provider = new IcalTokenAuthenticationProvider(icalTokenPort);
        filter = new IcalTokenAuthenticationFilter(new ProviderManager(provider));
    }

    @Nested
    @DisplayName("/ical/** with ?token= parameter")
    class IcalWithToken {

        @Test
        @DisplayName("valid token → authenticates and proceeds through chain")
        void validToken_authenticatesAndProceedsToChain() throws Exception {
            when(icalTokenPort.validate("validraw")).thenReturn(Optional.of(USER_ID));
            MockHttpServletRequest request = icalRequest("validraw");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication())
                    .isInstanceOf(IcalTokenAuthenticationToken.class)
                    .matches(auth -> auth.isAuthenticated());
            assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                    .isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("invalid token → 401 response, chain not called")
        void invalidToken_returns401() throws Exception {
            when(icalTokenPort.validate(anyString())).thenReturn(Optional.empty());
            MockHttpServletRequest request = icalRequest("badtoken");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verifyNoInteractions(filterChain);
            assertThat(response.getStatus()).isEqualTo(401);
        }

        @Test
        @DisplayName("unknown token → 401 response, chain not called")
        void unknownToken_returns401() throws Exception {
            when(icalTokenPort.validate(anyString())).thenReturn(Optional.empty());
            MockHttpServletRequest request = icalRequest("unknownToken1234567890");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verifyNoInteractions(filterChain);
            assertThat(response.getStatus()).isEqualTo(401);
        }
    }

    @Nested
    @DisplayName("/ical/** without token parameter")
    class IcalWithoutToken {

        @Test
        @DisplayName("no token param → falls through to chain, no auth set")
        void noToken_fallsThroughToChain() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ical/my-schedule.ics");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            verifyNoInteractions(icalTokenPort);
        }

        @Test
        @DisplayName("blank token param → falls through to chain")
        void blankToken_fallsThroughToChain() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ical/my-schedule.ics");
            request.setParameter("token", "");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(icalTokenPort);
        }
    }

    @Nested
    @DisplayName("/api/** paths — filter is inactive")
    class ApiPaths {

        @Test
        @DisplayName("token param on /api/** → filter skipped, no auth attempt")
        void tokenOnApiPath_filterSkipped() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
            request.setParameter("token", "validraw");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, filterChain);

            verify(filterChain).doFilter(request, response);
            verifyNoInteractions(icalTokenPort);
            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        }
    }

    private static MockHttpServletRequest icalRequest(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/ical/my-schedule.ics");
        request.setParameter("token", token);
        return request;
    }
}
