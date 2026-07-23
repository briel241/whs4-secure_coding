package com.tinyshop.auth.security;

import com.tinyshop.auth.service.IdentityNormalizer;
import com.tinyshop.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PlatformUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public PlatformUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        return users.findByNormalizedEmail(IdentityNormalizer.email(username))
            .map(PlatformPrincipal::new)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}
