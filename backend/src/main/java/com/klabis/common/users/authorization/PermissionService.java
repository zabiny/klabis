package com.klabis.common.users.authorization;

import com.klabis.common.users.Authority;
import com.klabis.common.users.UserId;
import com.klabis.common.users.UserPermissions;
import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Set;

@PrimaryPort
public interface PermissionService {

    UserPermissions updateUserPermissions(UserId userId, Set<Authority> newAuthorities);

    PermissionsResponse getUserPermissions(UserId userId);
}
