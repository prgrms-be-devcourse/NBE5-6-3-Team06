package com.grepp.matnam.app.model.outbox.repository;

import com.grepp.matnam.app.model.outbox.entity.OutBox;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutBoxRepository extends JpaRepository<OutBox, Long> {
    List<OutBox> findByActivatedIsTrueOrderByCreatedAt();
}
