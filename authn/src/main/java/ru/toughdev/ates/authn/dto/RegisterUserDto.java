package ru.toughdev.ates.authn.dto;

import lombok.Data;

@Data
public class RegisterUserDto {

    private String login;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String role;
}
