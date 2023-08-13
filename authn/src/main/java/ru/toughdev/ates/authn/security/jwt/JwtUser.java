package ru.toughdev.ates.authn.security.jwt;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

public class JwtUser extends User {

    private final UUID publicId;

    public JwtUser(UUID publicId, String login, String password, Collection<? extends GrantedAuthority> authorities) {
        super(login, password, authorities);
        this.publicId = publicId;
    }
}
