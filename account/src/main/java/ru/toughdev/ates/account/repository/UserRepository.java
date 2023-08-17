package ru.toughdev.ates.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.toughdev.ates.account.model.User;

import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    User getByPublicId(UUID publicId);
    User getByLogin(String login);
}