package com.resumeai.controller;

import com.resumeai.dto.CreateTemplateRequest;
import com.resumeai.dto.ResumeTemplateDetail;
import com.resumeai.dto.ResumeTemplateResponse;
import com.resumeai.dto.ResumeVersionDetail;
import com.resumeai.dto.ResumeVersionResponse;
import com.resumeai.dto.SaveVersionRequest;
import com.resumeai.dto.VersionCompareResponse;
import com.resumeai.model.UserAccount;
import com.resumeai.service.AuthService;
import com.resumeai.service.ResumeTemplateService;
import com.resumeai.service.ResumeVersionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 简历版本管理和模板库 API
 */
@RestController
@RequestMapping("/api")
public class ResumeVersionController {

    private final AuthService authService;
    private final ResumeVersionService resumeVersionService;
    private final ResumeTemplateService resumeTemplateService;

    public ResumeVersionController(
            AuthService authService,
            ResumeVersionService resumeVersionService,
            ResumeTemplateService resumeTemplateService) {
        this.authService = authService;
        this.resumeVersionService = resumeVersionService;
        this.resumeTemplateService = resumeTemplateService;
    }

    // ========== 简历版本管理 ==========

    @PostMapping("/resume/versions")
    public ResumeVersionDetail saveVersion(
            @RequestBody @Valid SaveVersionRequest req,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return toDetail(resumeVersionService.saveVersion(
                user.getId(),
                req.getTitle(),
                req.getContent(),
                req.getTargetRole(),
                "MANUAL",
                null
        ));
    }

    @GetMapping("/resume/versions")
    public ResumeVersionResponse listVersions(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return resumeVersionService.listVersions(user.getId(), page, size);
    }

    @GetMapping("/resume/versions/{id}")
    public ResumeVersionDetail getVersion(
            @PathVariable("id") Long id,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return resumeVersionService.getVersion(id, user.getId());
    }

    @DeleteMapping("/resume/versions/{id}")
    public Map<String, Object> deleteVersion(
            @PathVariable("id") Long id,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        resumeVersionService.deleteVersion(id, user.getId());
        return Map.of("ok", true);
    }

    @GetMapping("/resume/versions/compare")
    public VersionCompareResponse compareVersions(
            @RequestParam("id1") Long id1,
            @RequestParam("id2") Long id2,
            HttpServletRequest request) {
        UserAccount user = authService.requireUser(request);
        return resumeVersionService.compareVersions(id1, id2, user.getId());
    }

    // ========== 简历模板库 ==========

    @GetMapping("/resume/templates")
    public ResumeTemplateResponse listTemplates(
            @RequestParam(name = "industry", required = false) String industry,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return resumeTemplateService.listTemplates(industry, page, size);
    }

    @GetMapping("/resume/templates/{id}")
    public ResumeTemplateDetail getTemplate(@PathVariable("id") Long id) {
        return resumeTemplateService.getTemplate(id);
    }

    @PostMapping("/admin/resume/templates")
    public ResumeTemplateDetail createTemplate(
            @RequestBody @Valid CreateTemplateRequest req,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        return resumeTemplateService.createTemplate(req);
    }

    @PutMapping("/admin/resume/templates/{id}")
    public ResumeTemplateDetail updateTemplate(
            @PathVariable("id") Long id,
            @RequestBody @Valid CreateTemplateRequest req,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        return resumeTemplateService.updateTemplate(id, req);
    }

    @DeleteMapping("/admin/resume/templates/{id}")
    public Map<String, Object> deleteTemplate(
            @PathVariable("id") Long id,
            HttpServletRequest request) {
        authService.requireAdmin(request);
        resumeTemplateService.deleteTemplate(id);
        return Map.of("ok", true);
    }

    private ResumeVersionDetail toDetail(com.resumeai.model.ResumeVersion version) {
        ResumeVersionDetail detail = new ResumeVersionDetail();
        detail.setId(version.getId());
        detail.setTitle(version.getTitle());
        detail.setContent(version.getContent());
        detail.setTargetRole(version.getTargetRole());
        detail.setSourceType(version.getSourceType());
        detail.setSourceId(version.getSourceId());
        detail.setCreatedAt(version.getCreatedAt());
        detail.setUpdatedAt(version.getUpdatedAt());
        return detail;
    }
}
