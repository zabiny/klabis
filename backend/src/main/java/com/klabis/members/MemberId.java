package com.klabis.members;

import com.klabis.users.UserId;
import org.jmolecules.ddd.annotation.ValueObject;
import org.jmolecules.ddd.types.Identifier;

import java.util.UUID;

@ValueObject
public record MemberId(UUID value) implements Identifier {

    public UserId toUserId() {
        // memberId can be mapped directly to userId as these IDs have same uuid value.
        return new UserId(value);
    }

    public static MemberId fromUserId(UserId userId) {
        return new MemberId(userId.uuid());
    }
}
