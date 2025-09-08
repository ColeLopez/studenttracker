package com.cole.model;

import java.time.LocalDateTime;

public class RecentActivity {
    private int id;
    private int userId;
    private String activityType;
    private String description;
    private LocalDateTime activityTime;

    public RecentActivity(int id, int userId, String activityType, String description, LocalDateTime activityTime) {
        this.id = id;
        this.userId = userId;
        this.activityType = activityType;
        this.description = description;
        this.activityTime = activityTime;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getActivityType() {
        return activityType;
    }

    public void setActivityType(String activityType) {
        this.activityType = activityType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getActivityTime() {
        return activityTime;
    }

    public void setActivityTime(LocalDateTime activityTime) {
        this.activityTime = activityTime;
    }

    public void setUserDisplayName(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUserDisplayName'");
    }

    public void setUserRole(String string) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'setUserRole'");
    }
}
