package com.rhsystem.application.dto;

import java.util.List;
import java.util.Map;

public class TimeImportResponse {
    private List<TimeEventResponse> events;
    private int totalEvents;
    private int confirmedEvents;
    private int reviewEvents;
    private int uncertainEvents;
    private Map<String, List<String>> gradeByDate;
    private String message;

    public TimeImportResponse() {
    }

    public List<TimeEventResponse> getEvents() {
        return events;
    }

    public void setEvents(List<TimeEventResponse> events) {
        this.events = events;
    }

    public int getTotalEvents() {
        return totalEvents;
    }

    public void setTotalEvents(int totalEvents) {
        this.totalEvents = totalEvents;
    }

    public int getConfirmedEvents() {
        return confirmedEvents;
    }

    public void setConfirmedEvents(int confirmedEvents) {
        this.confirmedEvents = confirmedEvents;
    }

    public int getReviewEvents() {
        return reviewEvents;
    }

    public void setReviewEvents(int reviewEvents) {
        this.reviewEvents = reviewEvents;
    }

    public int getUncertainEvents() {
        return uncertainEvents;
    }

    public void setUncertainEvents(int uncertainEvents) {
        this.uncertainEvents = uncertainEvents;
    }

    public Map<String, List<String>> getGradeByDate() {
        return gradeByDate;
    }

    public void setGradeByDate(Map<String, List<String>> gradeByDate) {
        this.gradeByDate = gradeByDate;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
