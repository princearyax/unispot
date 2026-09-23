package com.prince.unispot.core.security;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

//need it at variours loc.. DRY
@Component
public class AuthorizationHelper {

    public void requireOwnerOrAdmin(Long resourceOwnerId, String message) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Long currentUserId = Long.valueOf(auth.getName());
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !resourceOwnerId.equals(currentUserId)) {
            throw new AccessDeniedException(message);
        }
    }
}