package com.sentinel.aml.security;

import com.sentinel.aml.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SentinelUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public SentinelUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository
                .findByUsername(username)
                .map(SentinelUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("No such user: " + username));
    }
}
