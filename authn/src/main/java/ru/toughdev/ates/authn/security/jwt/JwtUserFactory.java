package ru.toughdev.ates.authn.security.jwt;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.toughdev.ates.authn.model.User;

import java.util.List;

public final class JwtUserFactory {

    public static JwtUser create(User user) {
        return new JwtUser(
                user.getPublicId(),
                user.getLogin(),
                user.getPassword(),
                List.of(new SimpleGrantedAuthority(user.getRole())));
    }
}
