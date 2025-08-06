package com.cole.model;

import java.util.List;
import com.cole.controller.VirtualRecordCardController.FollowUp;
import com.cole.controller.VirtualRecordCardController.Note;

public class StudentReportData {
    private Student student;
    private List<StudentModule> modules; // modules with marks
    private List<Note> notes;
    private List<FollowUp> followUps;

    public StudentReportData(Student student, List<StudentModule> modules, List<Note> notes, List<FollowUp> followUps) {
        this.student = student;
        this.modules = modules;
        this.notes = notes;
        this.followUps = followUps;
    }

    // Getters and setters
    public Student getStudent() { return student; }
    public void setStudent(Student student) { this.student = student; }

    public List<StudentModule> getModules() { return modules; }
    public void setModules(List<StudentModule> modules) { this.modules = modules; }

    public List<Note> getNotes() { return notes; }
    public void setNotes(List<Note> notes) { this.notes = notes; }

    public List<FollowUp> getFollowUps() { return followUps; }
    public void setFollowUps(List<FollowUp> followUps) { this.followUps = followUps; }
}
