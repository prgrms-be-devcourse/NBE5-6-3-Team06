package com.grepp.matnam.app.model.team.repository;

import com.grepp.matnam.app.controller.api.admin.payload.SearchTeamResponse;
import com.grepp.matnam.app.model.team.code.ParticipantStatus;
import com.grepp.matnam.app.model.team.dto.MeetingDto;
import com.grepp.matnam.app.model.team.dto.MonthlyMeetingStatsDto;
import com.grepp.matnam.app.model.team.dto.ParticipantWithUserIdDto;
import com.grepp.matnam.app.model.team.dto.TeamDto;
import com.grepp.matnam.app.model.team.entity.QFavorite;
import com.grepp.matnam.app.model.team.entity.QParticipant;
import com.grepp.matnam.app.model.team.entity.QTeam;
import com.grepp.matnam.app.model.team.entity.Team;
import com.grepp.matnam.app.model.team.code.Status;
import com.grepp.matnam.app.model.user.entity.QUser;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

@RequiredArgsConstructor
public class TeamRepositoryCustomImpl implements TeamRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    private final QTeam team = QTeam.team;
    private final QParticipant participant = QParticipant.participant;
    private final QUser user = QUser.user;
    private final QFavorite favorite = QFavorite.favorite;

    @Override
    public Page<Team> findAllUsers(Pageable pageable) {
        List<Team> content = queryFactory
            .select(team)
            .from(team)
            .where(team.activated)
            .orderBy(getOrderSpecifiers(pageable.getSort()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(team.count())
            .where(team.activated)
            .from(team);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<Team> findByStatusAndKeywordContaining(String status, String keyword,
        Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(keyword)) {
            builder.and(
                team.user.userId.containsIgnoreCase(keyword)
                    .or(team.teamTitle.containsIgnoreCase(keyword))
            );
        }

        List<Team> content = queryFactory
            .select(team)
            .from(team)
            .where(team.activated)
            .where(builder.and(team.status.eq(Status.valueOf(status))))
            .orderBy(getOrderSpecifiers(pageable.getSort()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(team.count())
            .where(team.activated)
            .where(builder.and(team.status.eq(Status.valueOf(status))))
            .from(team);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<Team> findByStatus(String status, Pageable pageable) {
        List<Team> content = queryFactory
            .select(team)
            .from(team)
            .where(team.activated)
            .where(team.status.eq(Status.valueOf(status)))
            .orderBy(getOrderSpecifiers(pageable.getSort()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(team.count())
            .where(team.activated)
            .where(team.status.eq(Status.valueOf(status)))
            .from(team);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public Page<Team> findByKeywordContaining(String keyword, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (StringUtils.hasText(keyword)) {
            builder.and(
                team.user.userId.containsIgnoreCase(keyword)
                    .or(team.teamTitle.containsIgnoreCase(keyword))
            );
        }

        List<Team> content = queryFactory
            .select(team)
            .from(team)
            .where(team.activated)
            .where(builder)
            .orderBy(getOrderSpecifiers(pageable.getSort()))
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        JPAQuery<Long> countQuery = queryFactory
            .select(team.count())
            .where(team.activated)
            .where(builder)
            .from(team);

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    @Override
    public List<MonthlyMeetingStatsDto> findMonthlyMeetingStats(LocalDateTime startDate) {
        Expression<String> monthExpr = Expressions.stringTemplate("DATE_FORMAT({0}, '%Y-%m')", team.teamDate);
        NumberExpression<Long> completedMeetings = new CaseBuilder()
            .when(team.status.eq(Status.COMPLETED))
            .then(1L)
            .otherwise(0L)
            .sum();

        return queryFactory
            .select(Projections.constructor(
                MonthlyMeetingStatsDto.class,
                monthExpr,
                team.count(),
                completedMeetings
            ))
            .from(team)
            .where(
                team.teamDate.goe(startDate),
                team.activated.isTrue()
            )
            .groupBy(monthExpr)
            .orderBy(new OrderSpecifier<>(Order.ASC, monthExpr))
            .fetch();
    }

    @Override
    public double averageMaxPeopleForActiveTeams() {
        return Optional.ofNullable(
            queryFactory
                .select(team.maxPeople.avg())
                .from(team)
                .where(
                    team.status.in(Status.RECRUITING, Status.FULL),
                    team.activated.isTrue()
                )
                .fetchOne()
        ).orElse(0.0);
    }

    @Override
    public List<ParticipantWithUserIdDto> findAllDtoByTeamId(Long teamId) {
        return queryFactory
            .select(Projections.constructor(
                ParticipantWithUserIdDto.class,
                participant.participantId,
                user.userId
            ))
            .from(participant)
            .join(participant.user, user)
            .where(
                participant.team.teamId.eq(teamId),
                participant.participantStatus.eq(ParticipantStatus.APPROVED)
            )
            .fetch();
    }

    private OrderSpecifier<?>[] getOrderSpecifiers(Sort sort) {
        return sort.stream()
            .map(order -> {
                String property = order.getProperty();
                Order direction = order.isAscending() ? Order.ASC : Order.DESC;

                // createdAt만 정렬에 사용한다고 가정
                if ("createdAt".equals(property)) {
                    return new OrderSpecifier<>(direction, team.createdAt);
                }

                // 필요에 따라 다른 속성 추가
                throw new IllegalArgumentException("정렬 불가능한 속성: " + property);
            })
            .toArray(OrderSpecifier[]::new);
    }

    // 사용자 참가자 기준 팀 조회(activated = true)
    @Override
    public List<Team> findTeamsByParticipantUserIdAndParticipantStatusAndActivatedTrue(
        String userId,
        ParticipantStatus participantStatus
    ) {
        BooleanBuilder builder = new BooleanBuilder();
        builder
            .and(participant.user.userId.eq(userId))
            .and(participant.participantStatus.eq(participantStatus))
            .and(team.activated.isTrue())
            .and(participant.activated.isTrue());

        return queryFactory
            .select(team)
            .from(team)
            .join(team.participants, participant)
            .where(builder)
            .fetch();
    }

    // 페이징: activated=true, 상태가 COMPLETED/CANCELED 가 아닌 팀을 참여자 정보 포함하여 조회
    @Override
    public Page<Team> findAllWithParticipantsAndActivatedTrue(Pageable pageable, boolean includeCompleted, String keyword) {
        BooleanBuilder builder = new BooleanBuilder()
            .and(team.activated.isTrue())
            .and(team.status.ne(Status.CANCELED));
        if (!includeCompleted) {
            builder.and(team.status.ne(Status.COMPLETED));
        }
        if (StringUtils.hasText(keyword)) {
            builder.and(
                team.teamDetails.containsIgnoreCase(keyword)
                    .or(team.teamTitle.containsIgnoreCase(keyword))
            );
        }

        List<Team> content = queryFactory
            .selectDistinct(team)
            .from(team)
//            .leftJoin(team.participants, participant).fetchJoin()
            .where(builder)
            .orderBy(team.createdAt.desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        long total = Optional.ofNullable(
            queryFactory
                .select(team.count())
                .from(team)
                .where(builder)
                .fetchOne()
        ).orElse(0L);

        return PageableExecutionUtils.getPage(content, pageable, () -> total);
    }



    // 단건 조회: teamId 기준, activated=true, participants + user fetch join
    @Override
    public Optional<Team> findByIdWithParticipantsAndUserAndActivatedTrue(Long teamId) {
        Team result = queryFactory
            .select(team)
            .from(team)
            .leftJoin(team.participants, participant).fetchJoin()
            .leftJoin(participant.user, user).fetchJoin()
            .where(team.teamId.eq(teamId),
                team.activated.isTrue())
            .fetchOne();

        return Optional.ofNullable(result);
    }

    @Override
    public List<MeetingDto> findByTeamDateIn(LocalDate date) {
        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        return queryFactory
            .select(Projections.constructor(
                MeetingDto.class,
                team.teamId,
                team.teamTitle,
                participant.participantId,
                participant.user.userId
            ))
            .from(team)
            .join(team.participants, participant)
            .where(
                team.status.in(Status.RECRUITING, Status.FULL),
                participant.participantStatus.eq(ParticipantStatus.APPROVED),
                team.teamDate.between(startOfDay, endOfDay)
            )
            .fetch();
    }

    @Override
    public List<SearchTeamResponse> findTeamByKeyword(String keyword) {
        List<Tuple> raw = queryFactory
            .select(
                team.teamId,
                team.teamTitle,
                team.user.userId,
                participant.user.userId)
            .from(team)
            .join(team.participants, participant)
            .where(
                team.activated.isTrue(),
                team.user.userId.contains(keyword)
            )
            .fetch();

        // Java로 그룹핑
        Map<Long, SearchTeamResponse> result = new LinkedHashMap<>();

        for (Tuple row : raw) {
            Long teamId = row.get(team.teamId);
            String teamTitle = row.get(team.teamTitle);
            String leaderId = row.get(team.user.userId);
            String participantId = row.get(participant.user.userId);

            // 팀 ID를 키로 그룹핑
            result.computeIfAbsent(
                    teamId, k -> new SearchTeamResponse(leaderId, teamTitle, new ArrayList<>()))
                .getUserIds()
                .add(participantId);
        }

        return new ArrayList<>(result.values());
    }

    // 즐겨찾기 카운트
    @Override
    public Page<Team> findAllOrderByFavoriteCount(Pageable pageable, boolean includeCompleted, String keyword) {

        BooleanBuilder builder = new BooleanBuilder()
            .and(team.activated.eq(true))
            .and(team.status.ne(Status.CANCELED));
        if (!includeCompleted) {
            builder.and(team.status.ne(Status.COMPLETED));
        }
        if (StringUtils.hasText(keyword)) {
            builder.and(
                team.teamDetails.containsIgnoreCase(keyword)
                    .or(team.teamTitle.containsIgnoreCase(keyword))
            );
        }

        List<Tuple> tuples = queryFactory
            .select(team, favorite.count())
            .from(team)
            .leftJoin(team.favorites, favorite)
            .where(builder)
            .groupBy(team)
            .orderBy(favorite.count().desc())
            .offset(pageable.getOffset())
            .limit(pageable.getPageSize())
            .fetch();

        List<Team> teams = tuples.stream()
            .map(tuple -> {
                Team t = tuple.get(team);
                Long cnt = tuple.get(favorite.count());
                t.setFavoriteCount(cnt != null ? cnt : 0L);
                return t;
            })
            .toList();

        Long totalCount = queryFactory
            .select(team.count())
            .from(team)
            .where(builder)
            .fetchOne();
        long total = (totalCount != null ? totalCount : 0L);

        return new PageImpl<>(teams, pageable, total);
    }
}
