package com.cole.model;

import javafx.beans.property.SimpleBooleanProperty;

public class FollowUpRow {
    private int followUpId;
    private final String studentNumber;
    private final String studentName;
    private final String email;
    private final String phone;
    private final String description;
    private final String dueDate;
    private final SimpleBooleanProperty completed;

    public FollowUpRow(int followUpId, String studentNumber, String studentName, String email, String phone, String description, String dueDate, boolean completed) {
        this.followUpId = followUpId;
        this.studentNumber = studentNumber;
        this.studentName = studentName;
        this.email = email;
        this.phone = phone;
        this.description = description;
        this.dueDate = dueDate;
        this.completed = new SimpleBooleanProperty(completed);
    }

    public int getFollowUpId() { return followUpId; }
    
    public void setFollowUpId(int id) { this.followUpId = id; }

    public String getStudentNumber() {
        return studentNumber;
    }   

    public String getStudentName() {
        return studentName;
    }

    public String getEmail() {
        return email;
    }

    public String getPhone() {
        return phone;
    }

    public String getDescription() {
        return description;
    }

    public String getDueDate() {
        return dueDate;
    }

    public boolean isCompleted() { return completed.get(); }

    public void setCompleted(boolean value) { completed.set(value); }

    public SimpleBooleanProperty completedProperty() { return completed; }
}
