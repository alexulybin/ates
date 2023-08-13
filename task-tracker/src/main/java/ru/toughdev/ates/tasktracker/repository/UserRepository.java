package ru.toughdev.ates.tasktracker.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.toughdev.ates.tasktracker.model.User;

import java.util.List;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, String> {

    List<User> findByRoleNotIn(List<String> roles);
    boolean existsByPublicId(UUID publicId);
}