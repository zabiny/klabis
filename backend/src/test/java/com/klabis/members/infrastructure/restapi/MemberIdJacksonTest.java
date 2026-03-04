package com.klabis.members.infrastructure.restapi;

import com.klabis.members.MemberId;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JsonTest
class MemberIdJacksonTest {

    @Autowired
    private JacksonTester<MemberId> json;

    @Test
    void shouldSerializeMemberIdAsUuidString() throws Exception {
        UUID uuid = UUID.randomUUID();
        MemberId memberId = new MemberId(uuid);

        assertThat(json.write(memberId)).isEqualToJson("\"" + uuid + "\"");
    }

    @Test
    void shouldDeserializeMemberIdFromUuidString() throws Exception {
        UUID uuid = UUID.randomUUID();

        assertThat(json.parse("\"" + uuid + "\"")).isEqualTo(new MemberId(uuid));
    }
}
