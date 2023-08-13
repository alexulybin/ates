package ru.toughdev.ates.authn.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import ru.toughdev.ates.authn.dto.RegisterUserDto;
import ru.toughdev.ates.authn.kafka.MessageProducer;
import ru.toughdev.ates.authn.kafka.UserEvent;
import ru.toughdev.ates.authn.model.User;
import ru.toughdev.ates.authn.repository.UserRepository;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final UserRepository userRepository;
    private final MessageProducer messageProducer;
    private final BCryptPasswordEncoder passwordEncoder;

    @PreAuthorize("hasAuthority('admin')")
    @PostMapping
    public @ResponseBody User register(@RequestBody RegisterUserDto dto) throws JsonProcessingException {
        var user = User.builder()
                .login(dto.getLogin())
                .firstName(dto.getFirstName())
                .lastName(dto.getLastName())
                .email(dto.getEmail())
                .password(passwordEncoder.encode(dto.getPassword()))
                .role(dto.getRole())
                .build();

        var registeredUser = userRepository.saveAndFlush(user);

        var event = new UserEvent(
                "UserCreated",
                user.getPublicId(),
                user.getLogin(),
                user.getEmail(),
                user.getRole()
        );
        messageProducer.sendMessage(event, "user-stream");

        log.info("Registered User " + registeredUser);

        return registeredUser;
    }
}
