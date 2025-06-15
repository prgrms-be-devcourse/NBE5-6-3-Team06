package com.grepp.matnam.app.model.coupon.repository;

import com.grepp.matnam.app.model.coupon.code.CouponTemplateStatus;
import com.grepp.matnam.app.model.coupon.entity.CouponTemplate;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("SELECT ct FROM CouponTemplate ct JOIN FETCH ct.restaurant r " +
            "WHERE (:status IS NULL OR ct.status = :status) " +
            "AND (:keyword = '' OR ct.name LIKE %:keyword% OR r.name LIKE %:keyword%) " +
            "ORDER BY ct.createdAt DESC")
    Page<CouponTemplate> findByFilter(@Param("status") CouponTemplateStatus status,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    @Query("SELECT ct FROM CouponTemplate ct JOIN FETCH ct.restaurant r " +
            "WHERE ct.status = 'ACTIVE' " +
            "AND ct.startAt <= CURRENT_TIMESTAMP " +
            "AND ct.endAt > CURRENT_TIMESTAMP " +
            "AND ct.issuedQuantity < ct.totalQuantity " +
            "AND (:keyword = '' OR ct.name LIKE %:keyword% OR r.name LIKE %:keyword%)")
    Page<CouponTemplate> findAvailableCoupons(@Param("keyword") String keyword, Pageable pageable);
}