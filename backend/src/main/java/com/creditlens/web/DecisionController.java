package com.creditlens.web;

import com.creditlens.service.CreditApplicationService;
import com.creditlens.web.dto.DecisionPreviewResponse;
import com.creditlens.web.dto.PreviewRequest;
import com.creditlens.web.dto.StatsResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@Tag(name = "Decisions")
public class DecisionController {

    private final CreditApplicationService service;

    public DecisionController(CreditApplicationService service) {
        this.service = service;
    }

    @PostMapping("/decisions/preview")
    @Operation(summary = "What-if decision. Nothing is saved.")
    public DecisionPreviewResponse preview(@Valid @RequestBody PreviewRequest request) {
        return DecisionPreviewResponse.from(service.preview(request.toProfile()));
    }

    @GetMapping("/stats")
    @Operation(summary = "Portfolio counts for the dashboard")
    public StatsResponse stats() {
        return service.stats();
    }
}
