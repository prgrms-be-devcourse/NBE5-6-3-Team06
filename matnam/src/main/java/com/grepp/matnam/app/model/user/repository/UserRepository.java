package com.grepp.matnam.app.model.user.repository;

import com.grepp.matnam.app.model.user.code.Role;
import com.grepp.matnam.app.model.user.code.Gender;
import com.grepp.matnam.app.model.user.code.Status;
import com.grepp.matnam.app.model.user.entity.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String>, UserRepositoryCustom{
    Optional<User> findByEmail(String email);
    Optional<User> findByUserId(String userId);
    boolean existsByEmail(String email);
    boolean existsByUserId(String userId);
    List<User> findAllByStatusAndDueDateBefore(Status status, LocalDate date);
    long countByCreatedAtBefore(LocalDateTime of);

    long countByActivated(boolean activated);
    long countByCreatedAtAfter(LocalDateTime dateTime);
    long countByGender(Gender gender);
    long countByGenderAndActivated(Gender gender, boolean activated);
    long countByGenderAndCreatedAtAfter(Gender gender, LocalDateTime dateTime);

    List<User> findByRoleEqualsAndActivatedIsTrue(Role role);
}