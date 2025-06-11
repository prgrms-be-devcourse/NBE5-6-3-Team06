package com.grepp.matnam.app.model.user.repository;

import com.grepp.matnam.app.model.user.entity.Preference;
import com.grepp.matnam.app.model.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PreferenceRepository extends JpaRepository<Preference, Long>, PreferenceRepositoryCustom {
    Preference findByUser(User user);
}
