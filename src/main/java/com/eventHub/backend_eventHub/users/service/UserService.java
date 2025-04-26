package com.eventHub.backend_eventHub.users.service;


import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Optional;

/**
 * Servicio para gestionar usuarios y proveer datos para la autenticación.
 *
 * Implementa UserDetailsService para que Spring Security cargue los detalles del usuario desde MongoDB.
 */
@Slf4j
@Service
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }


    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userName) throws UsernameNotFoundException {
        Users user = userRepository.findByUserName(userName)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String roleName = user.getRole().getNombreRol().name();
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleName);
        return new org.springframework.security.core.userdetails.User(
                user.getUserName(),
                user.getPassword(),
                Collections.singleton(authority)
        );
    }

    public boolean existsByUserName(String username) {
        return userRepository.existsByUserName(username);
    }

    public void save(Users user) {
       userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public Optional<Users> findByUserName(String username) {
        return userRepository.findByUserName(username);
    }
}
