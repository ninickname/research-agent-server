package com.ninickname.summarizer.controller;

import com.ninickname.summarizer.model.ResearchResult;
import com.ninickname.summarizer.service.ResearchOrchestrator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamResearch(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int count) {
        return researchOrchestrator.researchWithProgress(topic, count);
    }
}