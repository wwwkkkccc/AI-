package com.resumeai.dto;

import java.util.List;

public class RedactionResponse {
    private String redactedText;
    private Integer redactedCount;
    private List<RedactedItem> redactedItems;

    public static class RedactedItem {
        private String type;
        private String original;
        private Integer position;

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getOriginal() {
            return original;
        }

        public void setOriginal(String original) {
            this.original = original;
        }

        public Integer getPosition() {
            return position;
        }

        public void setPosition(Integer position) {
            this.position = position;
        }
    }

    public String getRedactedText() {
        return redactedText;
    }

    public void setRedactedText(String redactedText) {
        this.redactedText = redactedText;
    }

    public Integer getRedactedCount() {
        return redactedCount;
    }

    public void setRedactedCount(Integer redactedCount) {
        this.redactedCount = redactedCount;
    }

    public List<RedactedItem> getRedactedItems() {
        return redactedItems;
    }

    public void setRedactedItems(List<RedactedItem> redactedItems) {
        this.redactedItems = redactedItems;
    }
}
