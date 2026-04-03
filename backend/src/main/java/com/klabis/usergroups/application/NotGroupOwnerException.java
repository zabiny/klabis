package com.klabis.usergroups.application;

import com.klabis.members.MemberId;
import com.klabis.usergroups.UserGroupId;

/**
 * @deprecated Moved to domain package. Will be removed in Phase 2. Use {@link com.klabis.usergroups.domain.NotGroupOwnerException} directly.
 */
@Deprecated
public class NotGroupOwnerException extends com.klabis.usergroups.domain.NotGroupOwnerException {

    public NotGroupOwnerException(MemberId memberId, UserGroupId groupId) {
        super(memberId, groupId);
    }
}
