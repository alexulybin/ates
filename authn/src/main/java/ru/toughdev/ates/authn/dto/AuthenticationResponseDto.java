package ru.toughdev.ates.authn.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthenticationResponseDto {

    private final String login;
    private final String token;
}
