package com.alibaba.cloud.ai.example.manus2.controller;

import com.alibaba.cloud.ai.example.manus2.model.*;
import com.alibaba.cloud.ai.example.manus2.service.AgentConfigService;
import com.alibaba.cloud.ai.example.manus2.service.AgentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
//import org.springframework.security.access.prepost.PreAuthorize;
//import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/agents")
@RequiredArgsConstructor
public class Agent2Controller {
    private final AgentConfigService agentConfigService;
    private final AgentsService agentsService;

    // 代理配置管理
    @PostMapping("/configs")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgentConfig> createConfig(@Valid @RequestBody AgentConfig config) {
        return ResponseEntity.ok(agentConfigService.createConfig(config));
    }

    @PutMapping("/configs/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AgentConfig> updateConfig(@PathVariable String id, @Valid @RequestBody AgentConfig config) {
        return ResponseEntity.ok(agentConfigService.updateConfig(id, config));
    }

    @DeleteMapping("/configs/{id}")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteConfig(@PathVariable String id) {
        agentConfigService.deleteConfig(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/configs")
    public ResponseEntity<List<AgentConfig>> getAllConfigs() {
        return ResponseEntity.ok(agentConfigService.findAllVisible());
    }

    @GetMapping("/configs/{id}")
    public ResponseEntity<AgentConfig> getConfig(@PathVariable String id) {
        return ResponseEntity.of(agentConfigService.findById(id));
    }

    @GetMapping("/configs/search")
    public ResponseEntity<List<AgentConfig>> searchConfigs(@RequestParam String name) {
        return ResponseEntity.ok(agentConfigService.findByName(name));
    }

    @PatchMapping("/configs/{id}/visibility")
//    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateConfigVisibility(@PathVariable String id, @RequestParam boolean visible) {
        agentConfigService.updateVisibility(id, visible);
        return ResponseEntity.ok().build();
    }

    // 代理实例管理
    @PostMapping("/instances")
    public ResponseEntity<Agent> createAgent(@RequestParam String configId) {
        return ResponseEntity.ok(agentsService.createAgent(configId));
    }

    @GetMapping("/instances/{id}")
    public ResponseEntity<Agent> getAgent(@PathVariable String id) {
        return ResponseEntity.of(agentsService.getAgent(id));
    }

    @PutMapping("/instances/{id}/state")
    public ResponseEntity<Agent> updateAgentState(@PathVariable String id, @RequestBody Map<String, Object> state) {
        return ResponseEntity.ok(agentsService.updateAgent(id, state));
    }

    @DeleteMapping("/instances/{id}")
    public ResponseEntity<Void> deleteAgent(@PathVariable String id) {
        agentsService.deleteAgent(id);
        return ResponseEntity.ok().build();
    }

    // 会话管理
    @PostMapping("/sessions")
    public ResponseEntity<Session> createSession(
            @RequestParam String agentId,
             User user) {
        return ResponseEntity.ok(agentsService.createSession(agentId, user.getId()));
    }

    @GetMapping("/sessions/{id}")
    public ResponseEntity<Session> getSession(@PathVariable String id) {
        return ResponseEntity.of(agentsService.getSession(id));
    }

    @PostMapping("/sessions/{id}/messages")
    public ResponseEntity<Message> sendMessage(
            @PathVariable String id,
            @RequestBody String content) {
        return ResponseEntity.ok(agentsService.sendMessage(id, content));
    }

    @GetMapping("/sessions/{id}/messages")
    public ResponseEntity<List<Message>> getSessionMessages(@PathVariable String id) {
        return ResponseEntity.ok(agentsService.getSessionMessages(id));
    }

    @PostMapping("/sessions/{id}/pause")
    public ResponseEntity<Void> pauseSession(@PathVariable String id) {
        agentsService.pauseSession(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{id}/resume")
    public ResponseEntity<Void> resumeSession(@PathVariable String id) {
        agentsService.resumeSession(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/sessions/{id}/end")
    public ResponseEntity<Void> endSession(@PathVariable String id) {
        agentsService.endSession(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/agents/{agentId}/sessions")
    public ResponseEntity<List<Session>> getAgentSessions(@PathVariable String agentId) {
        return ResponseEntity.ok(agentsService.getActiveSessions(agentId));
    }

    @GetMapping("/sessions")
    public ResponseEntity<List<Session>> getUserSessions( User user) {
        return ResponseEntity.ok(agentsService.getUserActiveSessions(user.getId()));
    }

    // 统计信息
    @GetMapping("/agents/{agentId}/stats")
    public ResponseEntity<Map<String, Integer>> getAgentStats(@PathVariable String agentId) {
        return ResponseEntity.ok(agentsService.getAgentStats(agentId));
    }
} 