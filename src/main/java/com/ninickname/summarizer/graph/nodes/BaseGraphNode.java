package com.ninickname.summarizer.graph.nodes;

import com.ninickname.summarizer.graph.NodeType;
import com.ninickname.summarizer.graph.ResearchGraphState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Abstract base class for graph nodes with common functionality.
 *
 * Best Practices:
 * - Template Method Pattern: Subclasses implement executeInternal()
 * - Automatic timing and error tracking
 * - SSE progress emission
 * - Consistent logging
 */
public abstract class BaseGraphNode implements GraphNode {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    protected final NodeType nodeType;

    protected BaseGraphNode(NodeType nodeType) {
        this.nodeType = nodeType;
    }

    @Override
    public ResearchGraphState execute(ResearchGraphState state) {
        long startTime = System.currentTimeMillis();
        logger.info("[{}] Starting execution...", nodeType.getId());

        try {
            // Emit progress event
            emitProgress(state, "step", nodeType.getId());

            // Execute node-specific logic
            ResearchGraphState newState = executeInternal(state);

            // Record timing
            long duration = System.currentTimeMillis() - startTime;
            logger.info("[{}] Completed in {}ms", nodeType.getId(), duration);

            // Update state with timing
            return newState.toBuilder()
                    .currentNode(nodeType)
                    .recordNodeDuration(nodeType, duration)
                    .build();

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logger.error("[{}] Failed after {}ms: {}", nodeType.getId(), duration, e.getMessage(), e);

            // Record error
            return state.toBuilder()
                    .currentNode(nodeType)
                    .recordNodeDuration(nodeType, duration)
                    .recordNodeError(nodeType, e.getMessage())
                    .build();
        }
    }

    /**
     * Node-specific execution logic.
     * Subclasses implement this method.
     */
    protected abstract ResearchGraphState executeInternal(ResearchGraphState state);

    @Override
    public NodeType getNodeType() {
        return nodeType;
    }

    /**
     * Emit SSE progress event if emitter is present
     */
    protected void emitProgress(ResearchGraphState state, String eventType, String data) {
        SseEmitter emitter = state.getEmitter();
        if (emitter == null) {
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(data));
        } catch (Exception e) {
            logger.warn("[{}] Failed to emit progress: {}", nodeType.getId(), e.getMessage());
        }
    }

    /**
     * Emit SSE progress with JSON data
     */
    protected void emitProgressJson(ResearchGraphState state, String eventType, Object data) {
        SseEmitter emitter = state.getEmitter();
        if (emitter == null) {
            return;
        }
        try {
            // Simple JSON serialization (can be enhanced with ObjectMapper if needed)
            emitter.send(SseEmitter.event()
                    .name(eventType)
                    .data(data));
        } catch (Exception e) {
            logger.warn("[{}] Failed to emit progress: {}", nodeType.getId(), e.getMessage());
        }
    }
}
