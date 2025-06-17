package com.grepp.matnam.app.model.coupon.repository;

import com.grepp.matnam.app.model.coupon.entity.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserCouponRepository extends JpaRepository<UserCoupon, Long> {

    @Query("select case when count(uc) > 0 then true else false end from UserCoupon uc where uc.user.userId = :userId and uc.couponTemplate.templateId = :templateId")
    boolean existsByUserIdAndCouponTemplateId(@Param("userId") String userId, @Param("templateId") Long templateId);

    Optional<UserCoupon> findByCouponCode(String couponCode);

    @Query("select uc from UserCoupon uc where uc.user.userId = :userId")
    List<UserCoupon> findAllByUserId(@Param("userId") String userId);

    @Query("SELECT uc.couponTemplate.templateId FROM UserCoupon uc WHERE uc.user.userId = :userId")
    Set<Long> findAppliedCouponTemplateIdsByUserId(@Param("userId") String userId);
}