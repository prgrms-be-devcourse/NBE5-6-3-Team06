package com.grepp.matnam.app.scheduler;

import com.grepp.matnam.app.facade.NotificationSender;
import com.grepp.matnam.app.model.notification.code.NotificationType;
import com.grepp.matnam.app.model.team.dto.MeetingDto;
import com.grepp.matnam.app.model.team.repository.TeamRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyMeetingScheduler {

    private final TeamRepository teamRepository;
    private final NotificationSender notificationSender;

    @Scheduled(cron = "0 0 0 * * *")
    public void notifyUpcomingMeetings() {
        LocalDate oneDayLater = LocalDate.now().plusDays(1);

        List<MeetingDto> participants = teamRepository.findByTeamDateIn(oneDayLater);
        log.info("=== 모임 일정 알림 === {}", participants);
        for (MeetingDto participant : participants) {
            notificationSender.sendNotificationToUser(participant.getUserId(),
                NotificationType.TEAM_MEET, "[" + participant.getTeamTitle() + "] 모임 만남 하루 전날입니다!",
                "/team/page/" + participant.getTeamId());
        }

    }

}
