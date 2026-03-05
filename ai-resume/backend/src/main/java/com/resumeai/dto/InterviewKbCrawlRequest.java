package com.resumeai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 面试知识库网页爬取请求 DTO，用于从指定 URL 爬取面试题并导入知识库
 */
public class InterviewKbCrawlRequest {
    /** 爬取的起始 URL（必填） */
    @NotBlank(message = "seed_url is required")
    private String seedUrl;

    /** 文档标题 */
    @Size(max = 255)
    private String title;

    /** 面试题主题 */
    @Size(max = 120)
    private String topic;

    /** 最大爬取页数，范围 1~80 */
    @Min(value = 1, message = "max_pages must be between 1 and 80")
    @Max(value = 80, message = "max_pages must be between 1 and 80")
    private Integer maxPages;

    /** 最大爬取深度，范围 0~3 */
    @Min(value = 0, message = "max_depth must be between 0 and 3")
    @Max(value = 3, message = "max_depth must be between 0 and 3")
    private Integer maxDepth;

    /** 是否仅爬取同域名下的页面 */
    private Boolean sameDomainOnly;

    /** 爬取时携带的认证 Cookie */
    @Size(max = 10000)
    private String authCookie;

    /** 爬取时携带的 Referer 头 */
    @Size(max = 255)
    private String referer;

    /** 爬取时携带的 Authorization 头 */
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
