package com.grepp.matnam.infra.ftstest;

import com.grepp.matnam.app.model.team.entity.Team;
import com.grepp.matnam.app.model.team.repository.TeamRepository;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {

    private final TeamRepository teamRepository;

    @Async
    public CompletableFuture<List<String>> test(String mode, String keyword) {
        int repeatCount = 10;
        List<String> result = IntStream.rangeClosed(1, repeatCount)
            .parallel()
            .mapToObj(i -> {
                long startTime = System.nanoTime();
                Page<Team> teams;
                if (mode.equals("LIKE")) {
                    teams = teamRepository.findAllWithParticipantsAndActivatedTrue(
                        PageRequest.of(1, 12, Sort.by(Sort.Direction.DESC, "createdAt")),
                        true, keyword);
                } else {
                    teams = teamRepository.findAllWithFullText(
                        PageRequest.of(1, 12, Sort.by(Sort.Direction.DESC, "createdAt")),
                        true, keyword);
                }
                long endTime = System.nanoTime();
                double durationInSeconds = (endTime - startTime) / 1_000_000_000.0;
                System.out.printf("%d번째 실행: %.6f초 (keyword: %s)%n",
                    i, durationInSeconds, keyword);
                return i + "번째: " + String.format("%.6f", durationInSeconds) + "초";
            })
            .collect(Collectors.toList());
        return CompletableFuture.completedFuture(result);
    }

}
