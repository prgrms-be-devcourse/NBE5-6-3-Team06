package com.grepp.matnam.app.model.restaurant.repository;

import com.grepp.matnam.app.model.restaurant.code.SuggestionStatus;
import com.grepp.matnam.app.model.restaurant.entity.QRestaurantSuggestion;
import com.grepp.matnam.app.model.restaurant.entity.RestaurantSuggestion;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class RestaurantSuggestionRepositoryCustomImpl implements RestaurantSuggestionRepositoryCustom{

    private final JPAQueryFactory queryFactory;
    private final QRestaurantSuggestion suggestion = QRestaurantSuggestion.restaurantSuggestion;

    @Override
    public Page<RestaurantSuggestion> findAllSuggestion(Pageable pageable) {
        List<RestaurantSuggestion> content = queryFactory
            .select(suggestion)
            .from(suggestion)
            .where(suggestion.activated)
            .orderBy(getOrderSpecifiers(pageable.getSort()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(suggestion.count())
            .where(suggestion.activated)
            .from(suggestion);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<RestaurantSuggestion> findByStatusAndKeywordContaining(String status,
        String keyword, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(keyword)) {
            builder.and(
                suggestion.submittedByUserId.containsIgnoreCase(keyword)
                    .or(suggestion.name.containsIgnoreCase(keyword))
            );
        }

        List<RestaurantSuggestion> content = queryFactory
            .select(suggestion)
            .from(suggestion)
            .where(suggestion.activated)
            .where(builder.and(suggestion.status.eq(SuggestionStatus.valueOf(status))))
            .orderBy(getOrderSpecifiers(pageable.getSort()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(suggestion.count())
            .where(suggestion.activated)
            .where(builder.and(suggestion.status.eq(SuggestionStatus.valueOf(status))))
            .from(suggestion);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<RestaurantSuggestion> findByStatus(String status, Pageable pageable) {
        List<RestaurantSuggestion> content = queryFactory
            .select(suggestion)
            .from(suggestion)
            .where(suggestion.activated)
            .where(suggestion.status.eq(SuggestionStatus.valueOf(status)))
            .orderBy(getOrderSpecifiers(pageable.getSort()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(suggestion.count())
            .where(suggestion.activated)
            .where(suggestion.status.eq(SuggestionStatus.valueOf(status)))
            .from(suggestion);


        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<RestaurantSuggestion> findByKeywordContaining(String keyword, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(keyword)) {
            builder.and(
                suggestion.submittedByUserId.containsIgnoreCase(keyword)
                    .or(suggestion.name.containsIgnoreCase(keyword))
            );
        }

        List<RestaurantSuggestion> content = queryFactory
            .select(suggestion)
            .from(suggestion)
            .where(suggestion.activated)
            .where(builder)
            .orderBy(getOrderSpecifiers(pageable.getSort()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(suggestion.count())
            .where(suggestion.activated)
            .where(builder)
            .from(suggestion);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort) {
        return sort.stream()
            .map(order -> {
                String property = order.getProperty();
                Order direction = order.isAscending() ? Order.ASC : Order.DESC;

                // createdAt만 정렬에 사용한다고 가정
                if ("createdAt".equals(property)) {
                    return new OrderSpecifier<>(direction, suggestion.submittedAt);
                }

                // 필요에 따라 다른 속성 추가
                throw new IllegalArgumentException("정렬 불가능한 속성: " + property);
            })
            .toArray(OrderSpecifier[]::new);
    }
}
