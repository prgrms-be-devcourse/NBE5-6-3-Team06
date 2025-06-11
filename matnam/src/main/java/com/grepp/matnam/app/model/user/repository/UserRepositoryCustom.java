package com.grepp.matnam.app.model.user.repository;

import com.grepp.matnam.app.model.user.code.Gender;
import com.grepp.matnam.app.model.user.entity.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserRepositoryCustom {

    Page<User> findAllUsers(Pageable pageable);

    Page<User> findByStatusAndKeywordContaining(String status, String keyword, Pageable pageable);

    Page<User> findByStatus(String status, Pageable pageable);

    Page<User> findByKeywordContaining(String keyword, Pageable pageable);

    List<Integer> findAllAges();

    List<Gender> findAllGenders();

    long countByStatusNotActive();

    long countByGenderAndStatusNotActive(Gender gender);
}
