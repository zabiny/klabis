package com.klabis.users.authorization;

import com.klabis.users.UserId;
import org.springframework.hateoas.RepresentationModel;

import java.util.Set;

/**
 * HATEOAS representation model for PermissionsResponse.
 */
public class PermissionsResponseModel extends RepresentationModel<PermissionsResponseModel> {
    private final UserId userId;
    private final Set<String> authorities;

    public PermissionsResponseModel(UserId userId, Set<String> authorities) {
        this.userId = userId;
        this.authorities = authorities;
    }

    public UserId getUserId() {
        return userId;
    }

    public Set<String> getAuthorities() {
        return authorities;
    }
}
