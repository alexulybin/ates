package ru.toughdev.ates.tasktracker.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

@Getter
public class JwtUser extends User {

    private final UUID publicId;

    public JwtUser(UUID publicId, String login, Collection<? extends GrantedAuthority> authorities) {
        super(login, "", authorities);
        this.publicId = publicId;
    }
}