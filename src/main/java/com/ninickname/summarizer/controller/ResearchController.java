package com.ninickname.summarizer.controller;

import com.ninickname.summarizer.model.ResearchResult;
import com.ninickname.summarizer.service.ResearchOrchestrator;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/research")
public class ResearchController {

    private final ResearchOrchestrator researchOrchestrator;

    public ResearchController(ResearchOrchestrator researchOrchestrator) {
        this.researchOrchestrator = researchOrchestrator;
    }

    @GetMapping
    public ResearchResult research(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int count) {
        return researchOrchestrator.research(topic, count);
    }
}