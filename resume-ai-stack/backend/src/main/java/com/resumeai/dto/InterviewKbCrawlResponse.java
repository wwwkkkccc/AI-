package com.resumeai.dto;

import java.util.ArrayList;
import java.util.List;

public class InterviewKbCrawlResponse {
    private String seedUrl;
    private int pagesVisited;
    private int pagesSucceeded;
    private int questionsExtracted;
    private int questionsImported;
    private InterviewKbDocItem doc;
    private List<String> errors = new ArrayList<>();

    public String getSeedUrl() {
        return seedUrl;
    }

    public void setSeedUrl(String seedUrl) {
        this.seedUrl = seedUrl;
    }

    public int getPagesVisited() {
        return pagesVisited;
    }

    public void setPagesVisited(int pagesVisited) {
        this.pagesVisited = pagesVisited;
    }

    public int getPagesSucceeded() {
        return pagesSucceeded;
    }

    public void setPagesSucceeded(int pagesSucceeded) {
        this.pagesSucceeded = pagesSucceeded;
    }

    public int getQuestionsExtracted() {
        return questionsExtracted;
    }

    public void setQuestionsExtracted(int questionsExtracted) {
        this.questionsExtracted = questionsExtracted;
    }

    public int getQuestionsImported() {
        return questionsImported;
    }

    public void setQuestionsImported(int questionsImported) {
        this.questionsImported = questionsImported;
    }

    public InterviewKbDocItem getDoc() {
        return doc;
    }

    public void setDoc(InterviewKbDocItem doc) {
        this.doc = doc;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
