package com.ninickname.summarizer.controller;

import com.ninickname.summarizer.graph.ResearchGraphService;
import com.ninickname.summarizer.model.ResearchResult;
import com.ninickname.summarizer.service.ResearchOrchestrator;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/research")
public class ResearchController {

    private final ResearchOrchestrator researchOrchestrator;
    private final ResearchGraphService researchGraphService;

    public ResearchController(ResearchOrchestrator researchOrchestrator,
                              ResearchGraphService researchGraphService) {
        this.researchOrchestrator = researchOrchestrator;
        this.researchGraphService = researchGraphService;
    }

    @GetMapping
    public ResearchResult research(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "false") boolean skipContentFetch) {
        return researchOrchestrator.research(topic, count, skipContentFetch);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamResearch(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "false") boolean skipContentFetch) {
        return researchOrchestrator.researchWithProgress(topic, count, skipContentFetch);
    }

    // Graph-based endpoints (new implementation)

    @GetMapping("/graph")
    public ResearchResult researchGraph(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "false") boolean skipContentFetch) {
        return researchGraphService.research(topic, count, skipContentFetch);
    }

    @GetMapping(value = "/graph/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamResearchGraph(
            @RequestParam String topic,
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(defaultValue = "false") boolean skipContentFetch) {
        return researchGraphService.researchWithProgress(topic, count, skipContentFetch);
    }
}