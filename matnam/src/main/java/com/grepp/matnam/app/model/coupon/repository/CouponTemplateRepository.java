package com.grepp.matnam.app.model.coupon.repository;

import com.grepp.matnam.app.model.coupon.entity.CouponTemplate;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CouponTemplateRepository extends JpaRepository<CouponTemplate, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from CouponTemplate t where t.templateId = :id")
    Optional<CouponTemplate> findByIdWithPessimisticLock(@Param("id") Long id);
}