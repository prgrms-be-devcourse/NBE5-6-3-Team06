package com.grepp.matnam.app.scheduler;

import com.grepp.matnam.app.model.kafka.service.KafkaProducerService;
import com.grepp.matnam.app.model.outbox.entity.OutBox;
import com.grepp.matnam.app.model.outbox.repository.OutBoxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@RequiredArgsConstructor
@EnableScheduling
public class OutBoxPollingScheduler {

    private final OutBoxRepository outBoxRepository;
    private final KafkaProducerService kafkaProducerService;

    @Transactional
    @Scheduled(fixedDelay = 5000)
    public void pollAndPublish() {
        List<OutBox> unpublished = outBoxRepository.findByActivatedIsTrueOrderByCreatedAt();

        if(unpublished.isEmpty()) {
            return;
        }

        log.info("처리 outbox 이벤트 : {}", unpublished.size());

        unpublished.forEach(outBox -> {
            try{
                if("SIGNUP_COMPLETE".equals(outBox.getEvent())){
                    kafkaProducerService.sendSignupEvent(
                        outBox.getEmail(),
                        outBox.getEmailCode(),
                        outBox.getNickName()
                    );
                }
                // 성공시 -> false
                outBox.unActivated();
                log.info("kafka 이벤트 발행 완료");
            }catch(Exception e){
                log.error("kafka 이벤트 발행 실패 : {}",e.getMessage(),e);
            }
        });
    }

}
