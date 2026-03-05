package com.resumeai.dto;

public class InterviewKbLlmImportResponse {
    private String topic;
    private String model;
    private int requestedCount;
    private int generatedCount;
    private int importedCount;
    private InterviewKbDocItem doc;

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getRequestedCount() {
        return requestedCount;
    }

    public void setRequestedCount(int requestedCount) {
        this.requestedCount = requestedCount;
    }

    public int getGeneratedCount() {
        return generatedCount;
    }

    public void setGeneratedCount(int generatedCount) {
        this.generatedCount = generatedCount;
    }

    public int getImportedCount() {
        return importedCount;
    }

    public void setImportedCount(int importedCount) {
        this.importedCount = importedCount;
    }

    public InterviewKbDocItem getDoc() {
        return doc;
    }

    public void setDoc(InterviewKbDocItem doc) {
        this.doc = doc;
    }
}
