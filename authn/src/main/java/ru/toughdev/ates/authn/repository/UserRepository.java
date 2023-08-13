package ru.toughdev.ates.authn.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.toughdev.ates.authn.model.User;

public interface UserRepository extends JpaRepository<User, String> {
    User findByLogin(String login);
}