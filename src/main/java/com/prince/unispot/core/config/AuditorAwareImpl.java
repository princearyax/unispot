package com.prince.unispot.core.config;

import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component("auditorAwareImpl") //this name used in JPAConfig
public class AuditorAwareImpl implements AuditorAware<Long> {

    @Override
    public Optional<Long> getCurrentAuditor() {
        //  get the cur from the active thread, ThreadLocal, here its VT
        //older used threadlocal and we must ensure, when switch threaddlocal gets cleared so that other cant mistakenly identify as other!
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        //if not logged
        if (authentication == null 
            || !authentication.isAuthenticated() 
            || "anonymousUser".equals(authentication.getPrincipal())
        ) {
            return Optional.empty();
        }

        // int jwt filter, we'll  set the Subject (getName) to userId, so fine
        try {
            return Optional.of(Long.valueOf(authentication.getName()));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}