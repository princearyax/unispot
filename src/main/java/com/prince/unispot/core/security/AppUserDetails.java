package com.prince.unispot.core.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.Collections;

//necessry to get my own, so that when login i can get a hold for this and use the object alrady authN rather than db query again
public record AppUserDetails(
    Long id,
    String email,
    String password,
    String role
) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role));
    }

    @Override
    public String getPassword() { return password; }

    @Override
    public String getUsername() { return email; }

    //remaining methods returning true ..isAccountNonExpire, well spring already has defaults
}