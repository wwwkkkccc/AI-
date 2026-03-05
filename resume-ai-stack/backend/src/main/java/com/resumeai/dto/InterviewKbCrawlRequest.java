package com.resumeai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class InterviewKbCrawlRequest {
    @NotBlank(message = "seed_url is required")
    private String seedUrl;

    @Size(max = 255)
    private String title;

    @Size(max = 120)
    private String topic;

    @Min(value = 1, message = "max_pages must be between 1 and 80")
    @Max(value = 80, message = "max_pages must be between 1 and 80")
    private Integer maxPages;

    @Min(value = 0, message = "max_depth must be between 0 and 3")
    @Max(value = 3, message = "max_depth must be between 0 and 3")
    private Integer maxDepth;

    private Boolean sameDomainOnly;

    @Size(max = 10000)
    private String authCookie;

    @Size(max = 255)
    private String referer;

    @Size(max = 1024)
    private String authHeader;

    public String getSeedUrl() {
        return seedUrl;
    }

    public void setSeedUrl(String seedUrl) {
        this.seedUrl = seedUrl;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(Integer maxPages) {
        this.maxPages = maxPages;
    }

    public Integer getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(Integer maxDepth) {
        this.maxDepth = maxDepth;
    }

    public Boolean getSameDomainOnly() {
        return sameDomainOnly;
    }

    public void setSameDomainOnly(Boolean sameDomainOnly) {
        this.sameDomainOnly = sameDomainOnly;
    }

    public String getAuthCookie() {
        return authCookie;
    }

    public void setAuthCookie(String authCookie) {
        this.authCookie = authCookie;
    }

    public String getReferer() {
        return referer;
    }

    public void setReferer(String referer) {
        this.referer = referer;
    }

    public String getAuthHeader() {
        return authHeader;
    }

    public void setAuthHeader(String authHeader) {
        this.authHeader = authHeader;
    }
}
