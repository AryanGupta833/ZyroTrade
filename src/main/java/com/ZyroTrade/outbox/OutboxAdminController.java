package com.ZyroTrade.outbox;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/outbox")
@PreAuthorize("hasRole('ADMIN')")
@io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "bearerAuth")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Outbox administration")
public class OutboxAdminController {
    private final OutboxService outboxService;

    public OutboxAdminController(OutboxService outboxService) {
        this.outboxService = outboxService;
    }

    @GetMapping
    public List<OutboxEventSummary> events() {
        return outboxService.latest();
    }

    @PostMapping("/{id}/replay")
    public void replay(@PathVariable Long id) {
        outboxService.replay(id);
    }
}
