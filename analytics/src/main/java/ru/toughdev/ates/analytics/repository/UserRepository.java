package ru.toughdev.ates.analytics.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.toughdev.ates.analytics.model.User;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, Long> {

    User getByPublicId(UUID publicId);
}