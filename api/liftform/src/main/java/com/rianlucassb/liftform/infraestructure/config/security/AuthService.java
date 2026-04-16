package com.rianlucassb.liftform.infraestructure.config.security;

import com.rianlucassb.liftform.core.gateway.user.UserRepository;
import com.rianlucassb.liftform.core.domain.model.User;
import com.rianlucassb.liftform.infraestructure.adapter.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserMapper mapper;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        String normalized = login.trim().toLowerCase();

        User user = userRepository.findByEmail(normalized)
                .or(() -> userRepository.findByUsername(normalized))
                .orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));

        return mapper.toEntity(user);
    }
}