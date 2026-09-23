package com.klabis.members.infrastructure.orissync;

import com.dpolach.api.orisclient.OrisApiClient;
import com.dpolach.api.orisclient.dto.ClubMember;
import com.klabis.common.settings.OrisClubKeyAccessor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultOrisClubMembers")
class DefaultOrisClubMembersTest {

    @Mock
    private OrisApiClient orisApiClient;

    private final StubClubKeyAccessor clubKeyPort = new StubClubKeyAccessor();

    private OrisClubMembers orisClubMembers;

    @BeforeEach
    void setUp() {
        orisClubMembers = new DefaultOrisClubMembers(orisApiClient, clubKeyPort);
    }

    @Test
    @DisplayName("throws ClubKeyNotSetException and never calls OrisApiClient when no key is held")
    void throwsWhenNoKeyHeld() {
        assertThatThrownBy(() -> orisClubMembers.listClubMembers())
                .isInstanceOf(ClubKeyNotSetException.class);

        verify(orisApiClient, never()).getClubUserList(anyString());
    }

    @Test
    @DisplayName("passes the held key through to getClubUserList when one is set")
    void passesHeldKeyThrough() {
        clubKeyPort.store("secret-club-key");
        Map<String, ClubMember> members = Map.of();
        OrisApiClient.OrisResponse<Map<String, ClubMember>> response =
                new OrisApiClient.OrisResponse<>(members, "JSON", "OK", null, "getClubUserList");
        when(orisApiClient.getClubUserList("secret-club-key")).thenReturn(response);

        Map<String, ClubMember> result = orisClubMembers.listClubMembers();

        assertThat(result).isEqualTo(members);
        verify(orisApiClient).getClubUserList("secret-club-key");
    }

    /**
     * Minimal in-memory accessor for this test: the shared adapter is package-private to
     * {@code common.settings}, and only its read side matters here.
     */
    private static final class StubClubKeyAccessor implements OrisClubKeyAccessor {

        private String key;

        @Override
        public boolean isSet() {
            return key != null;
        }

        @Override
        public String currentKey() {
            return key;
        }

        void store(String key) {
            this.key = key;
        }
    }
}
