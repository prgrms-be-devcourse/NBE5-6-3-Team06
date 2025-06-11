package com.grepp.matnam.app.model.mymap.repository;

import com.grepp.matnam.app.model.mymap.entity.Mymap;
import com.grepp.matnam.app.model.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MymapRepository extends JpaRepository<Mymap, Long>, MymapRepositoryCustom {

    List<Mymap> findByUserAndActivatedTrue(User user);

    long countByUserAndActivatedTrue(User user);

    long countByUserAndActivatedTrueAndPinnedTrue(User user);

    long countByUserAndActivatedTrueAndPinnedFalse(User user);
}
