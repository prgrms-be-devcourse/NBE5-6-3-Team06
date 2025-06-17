package com.grepp.matnam.app.model.team.repository;

import com.grepp.matnam.app.model.team.code.ParticipantStatus;
import com.grepp.matnam.app.model.team.entity.Participant;
import com.grepp.matnam.app.model.team.entity.QParticipant;
import com.grepp.matnam.app.model.user.entity.QUser;
import com.querydsl.jpa.impl.JPAQueryFactory;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ParticipantRepositoryCustomImpl implements ParticipantRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    private final QParticipant participant = QParticipant.participant;
    private final QUser user = QUser.user;

    @Override
    public long countApprovedExcludingHost(Long teamId, ParticipantStatus status) {
        Long count = queryFactory
            .select(participant.count())
            .from(participant)
            .where(
                participant.team.teamId.eq(teamId),
                participant.participantStatus.eq(status),
                participant.user.userId.ne(participant.team.user.userId)
            )
            .fetchOne();

        return count != null ? count : 0L;
    }

    @Override
    public List<Participant> findParticipantsWithUserByTeamId(Long teamId) {
        return queryFactory
            .select(participant)
            .from(participant)
            .join(participant.user, user).fetchJoin()
            .where(
                participant.team.teamId.eq(teamId),
                participant.activated.isTrue()
            )
            .fetch();
    }

}
