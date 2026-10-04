package com.klabis.common.authorization;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;

import java.util.Map;
import java.util.Set;

/**
 * SPI through which a module contributes grants derived from a relationship ("user U holds authority A over
 * targets T"). Grants of all sources are united. Returns every grant of the user at once so the snapshot costs
 * one query per source and request.
 * <p>
 * Grants for authorities that cannot be held over specific targets are discarded by the snapshot.
 */
public interface RelationshipSource {

    Map<Authority, Set<TargetRef>> grantsOf(UserId userId);
}
