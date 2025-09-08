package com.cole.model;

import java.time.LocalDate;

public class ToDoTask {
    private int id;
    private int userId;
    private String taskText;
    private LocalDate dueDate;
    private boolean completed;  
    private String note;
    private String priority; // e.g., "Low", "Medium", "High"
    private String recurring; // e.g., "DAILY", "WEEKLY", "MONTHLY", or null
    private boolean active = true;
    private Integer parentId; // ID of the parent task if this is a subtask

    public ToDoTask(int id, int userId, String taskText, 
            LocalDate dueDate, boolean completed, String note, String priority, String recurring) {
        this.id = id;
        this.userId = userId;
        this.taskText = taskText;
        this.dueDate = dueDate;
        this.completed = completed;
        this.note = note;
        this.priority = priority;
        this.recurring = recurring;
    }
    
    public int getId() {
        return id;
    }
    
    public int getUserId() {
        return userId;
    }
    
    public String getTaskText() {
        return taskText;
    }
    
    public LocalDate getDueDate() {
        return dueDate;
    }
    
    public boolean isCompleted() {
        return completed;
    }
    
    public String getNote() {
        return note;
    }

    public String getPriority() {
        return priority;
    }
    
    public String getRecurring() {
        return recurring;
    }
    
    public boolean isActive() {
        return active;
    }

    public void setId(int id) {
        this.id = id;
    }
    
    public void setUserId(int userId) {
        this.userId = userId;
    }

    public void setTaskText(String taskText) {
        this.taskText = taskText;
    }
    
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
    
    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
    
    public void setNote(String note) {
        this.note = note;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public void setRecurring(String recurring) {
        this.recurring = recurring;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setParentId(Integer parentId) {
        this.parentId = parentId;
    }
    public Integer getParentId() {
        return parentId;
    }
}
