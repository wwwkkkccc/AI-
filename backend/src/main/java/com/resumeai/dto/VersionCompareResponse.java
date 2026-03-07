package com.resumeai.dto;

import java.util.List;

/**
 * 简历版本对比响应
 */
public class VersionCompareResponse {
    private Long id1;
    private String title1;
    private Long id2;
    private String title2;
    private List<DiffLine> lines;

    public Long getId1() {
        return id1;
    }

    public void setId1(Long id1) {
        this.id1 = id1;
    }

    public String getTitle1() {
        return title1;
    }

    public void setTitle1(String title1) {
        this.title1 = title1;
    }

    public Long getId2() {
        return id2;
    }

    public void setId2(Long id2) {
        this.id2 = id2;
    }

    public String getTitle2() {
        return title2;
    }

    public void setTitle2(String title2) {
        this.title2 = title2;
    }

    public List<DiffLine> getLines() {
        return lines;
    }

    public void setLines(List<DiffLine> lines) {
        this.lines = lines;
    }

    public static class DiffLine {
        private String type; // SAME, ADDED, REMOVED
        private String text;

        public DiffLine(String type, String text) {
            this.type = type;
            this.text = text;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }
    }
}
