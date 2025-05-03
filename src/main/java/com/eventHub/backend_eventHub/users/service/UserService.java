package com.eventHub.backend_eventHub.users.service;


import com.eventHub.backend_eventHub.domain.entities.State;
import com.eventHub.backend_eventHub.domain.entities.Users;
import com.eventHub.backend_eventHub.users.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class UserService {
     private final UserRepository userRepository;

     @Autowired
     public UserService(UserRepository userRepository) {
         this.userRepository = userRepository;
     }

    /**
     * Obtiene todos los usuarios o filtra por estado.
     */


}
