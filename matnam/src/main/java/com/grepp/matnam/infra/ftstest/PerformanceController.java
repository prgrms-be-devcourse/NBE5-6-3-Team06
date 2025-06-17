package com.grepp.matnam.infra.ftstest;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@RequestMapping("performance")
public class PerformanceController {

    private final PerformanceService performanceService;

    @GetMapping("fts")
    public String test(
        @RequestParam(name = "keyword", defaultValue = "") String keyword,
        Model model) throws ExecutionException, InterruptedException {

        if (!keyword.isBlank()) {
            CompletableFuture<List<String>> likeResults = performanceService.test("LIKE", keyword);
            CompletableFuture<List<String>> fullTextResults = performanceService.test("FTS", keyword);
            model.addAttribute("likeResults", likeResults.get());
            model.addAttribute("fullTextResults", fullTextResults.get());
            model.addAttribute("testCount", likeResults.get().size());
        }

        return "performance/fts";
    }
}
