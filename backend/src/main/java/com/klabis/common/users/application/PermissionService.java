package com.klabis.common.users.application;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.domain.UserPermissions;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Set;

@PrimaryPort
public interface PermissionService {

    UserPermissions updateUserPermissions(UserId userId, Set<Authority> newAuthorities);

    UserPermissions getUserPermissions(UserId userId);
}
