package com.learn.user_service.service;

import com.learn.user_service.dto.UserDTO;
import com.learn.user_service.exception.DuplicateResourceException;
import com.learn.user_service.exception.ResourceNotFoundException;
import com.learn.user_service.kafka.UserEventProducer;
import com.learn.user_service.model.User;
import com.learn.user_service.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class UserService {
    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserEventProducer userEventProducer;

    public List<User> getAllUsers() {
        log.debug("Fetching all users");
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        log.debug("Fetching user with id={}", id);
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    public User createUser(UserDTO dto) {
        log.info("Creating user with email={}", dto.getEmail());

        userRepository.findByEmail(dto.getEmail()).ifPresent(existing -> {
            log.warn("Attempted to create duplicate user with email={}", dto.getEmail());
            throw new DuplicateResourceException("A user with email " + dto.getEmail() + " already exists");
        });

        User user = new User(dto.getName(), dto.getEmail());
        User saved = userRepository.save(user);
        log.info("User created successfully with id={}", saved.getId());

        userEventProducer.sendUserCreatedEvent(saved);
        return saved;
    }

    public void deleteUser(Long id) {
        log.info("Deleting user with id={}", id);
        User user = getUserById(id);
        userRepository.delete(user);
        log.info("User with id={} deleted", id);
    }
}
