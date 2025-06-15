package com.grepp.matnam.app.coupon;

import com.grepp.matnam.app.model.coupon.code.CouponTemplateStatus;
import com.grepp.matnam.app.model.coupon.entity.CouponTemplate;
import com.grepp.matnam.app.model.coupon.repository.CouponTemplateRepository;
import com.grepp.matnam.app.model.coupon.repository.UserCouponRepository;
import com.grepp.matnam.app.model.coupon.service.CouponIssueService;
import com.grepp.matnam.app.model.restaurant.entity.Restaurant;
import com.grepp.matnam.app.model.restaurant.repository.RestaurantRepository;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.app.model.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CouponIssueIntegrationTest {

    @Autowired
    private CouponIssueService couponIssueService;
    @Autowired
    private UserCouponRepository userCouponRepository;
    @Autowired
    private CouponTemplateRepository couponTemplateRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RestaurantRepository restaurantRepository;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private CouponTemplate activeCouponTemplate;
    private List<User> users;

    @BeforeEach
    void setUp() {
        Restaurant testRestaurant = new Restaurant();
        testRestaurant.setName("테스트 식당");
        restaurantRepository.save(testRestaurant);

        // 100명의 테스트 사용자 생성
        users = IntStream.range(0, 100)
                .mapToObj(i -> {
                    User user = new User();
                    user.setUserId("testuser" + i);
                    user.setEmail("test" + i + "@test.com");
                    user.setNickname("user" + i);
                    return user;
                })
                .collect(Collectors.toList());
        userRepository.saveAll(users);


        // 수량이 10개인 쿠폰 템플릿 생성
        activeCouponTemplate = couponTemplateRepository.save(CouponTemplate.builder()
                .name("선착순 10명 쿠폰")
                .totalQuantity(10)
                .issuedQuantity(0)
                .restaurant(testRestaurant)
                .discountType(com.grepp.matnam.app.model.coupon.code.DiscountType.FIXED_AMOUNT)
                .discountValue(1000)
                .startAt(LocalDateTime.now().minusDays(1))
                .endAt(LocalDateTime.now().plusDays(1))
                .status(CouponTemplateStatus.ACTIVE)
                .validDays(30)
                .build());

        redisTemplate.opsForSet().add("coupon:active_campaigns", String.valueOf(activeCouponTemplate.getTemplateId()));
    }

    @AfterEach
    void tearDown() {
        userCouponRepository.deleteAll();
        couponTemplateRepository.deleteAll();
        userRepository.deleteAll();
        restaurantRepository.deleteAll();
        redisTemplate.delete(redisTemplate.keys("coupon:*"));
    }

    @Test
    @DisplayName("100명의 사용자가 동시에 10개의 쿠폰에 응모하면, 정확히 10개만 발급된다.")
    void issue_10_coupons_for_100_concurrent_requests() throws InterruptedException {
        // given
        int numberOfThreads = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(numberOfThreads);

        // when
        // 100명의 사용자가 동시에 쿠폰 발급 요청
        for (User user : users) {
            executorService.submit(() -> {
                try {
                    couponIssueService.applyForCoupon(user.getUserId(), activeCouponTemplate.getTemplateId());
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await(); // 모든 요청이 완료될 때까지 대기

        // 스케줄러가 대기열을 처리할 시간을 줌
        Thread.sleep(7000);

        // then
        // 1. 발급된 사용자 쿠폰의 총 개수는 10개여야 한다.
        long issuedCouponCount = userCouponRepository.count();
        assertEquals(10, issuedCouponCount);

        // 2. 쿠폰 템플릿의 발급된 수량도 10개여야 한다.
        CouponTemplate finishedTemplate = couponTemplateRepository.findById(activeCouponTemplate.getTemplateId()).orElseThrow();
        assertEquals(10, finishedTemplate.getIssuedQuantity());

        // 3. 쿠폰 템플릿의 상태가 '소진됨(EXHAUSTED)'으로 변경되어야 한다.
        assertEquals(CouponTemplateStatus.EXHAUSTED, finishedTemplate.getStatus());
    }

}