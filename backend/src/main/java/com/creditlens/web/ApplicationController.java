package com.creditlens.web;

import com.creditlens.domain.ApplicationStatus;
import com.creditlens.domain.CreditApplication;
import com.creditlens.service.CreditApplicationService;
import com.creditlens.web.dto.ApplicationResponse;
import com.creditlens.web.dto.AuditResponse;
import com.creditlens.web.dto.CreateApplicationRequest;
import com.creditlens.web.dto.PageResponse;
import com.creditlens.web.dto.ReviewRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/** Thin HTTP layer: parse + validate input, call the service, map to DTOs, pick status codes. */
@RestController
@RequestMapping("/api/applications")
@Tag(name = "Credit applications")
public class ApplicationController {

    private final CreditApplicationService service;

    public ApplicationController(CreditApplicationService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Submit an application; the rules engine decides immediately")
    public ResponseEntity<ApplicationResponse> submit(@Valid @RequestBody CreateApplicationRequest request) {
        CreditApplication app = service.submit(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(app.getId()).toUri();
        return ResponseEntity.created(location).body(ApplicationResponse.from(app)); // 201 + Location
    }

    @GetMapping
    @Operation(summary = "List applications, newest first, optionally filtered by status")
    public PageResponse<ApplicationResponse> list(
            @RequestParam(required = false) ApplicationStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100); // cap page size: protects the database
        PageRequest pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return PageResponse.from(service.list(status, pageable), ApplicationResponse::from);
    }

    @GetMapping("/{id}")
    public ApplicationResponse get(@PathVariable Long id) {
        return ApplicationResponse.from(service.get(id));
    }

    @GetMapping("/{id}/audit")
    @Operation(summary = "Full decision history for one application")
    public List<AuditResponse> audit(@PathVariable Long id) {
        return service.auditTrail(id).stream().map(AuditResponse::from).toList();
    }

    @PostMapping("/{id}/review")
    @Operation(summary = "Underwriter approves or declines a REFERRED application")
    public ApplicationResponse review(@PathVariable Long id, @Valid @RequestBody ReviewRequest request) {
        return ApplicationResponse.from(service.review(id, request));
    }
}
