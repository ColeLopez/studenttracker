package com.cole.model;

public class StudentModule {
    private int studentId;
    private int moduleId;
    private String moduleCode;
    private String moduleName;
    private String examType;
    private boolean receivedBook;
    private Double formative;
    private Double summative;
    private Double supplementary;

    public StudentModule(int studentId, int moduleId, String moduleCode, String moduleName, String examType, boolean receivedBook) {
        this(studentId, moduleId, moduleCode, moduleName, receivedBook, null, null, null);
    }

    public StudentModule(int studentId, int moduleId, String moduleCode, String moduleName, boolean receivedBook, Double formative, Double summative, Double supplementary) {
        this.studentId = studentId;
        this.moduleId = moduleId;
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.receivedBook = receivedBook;
        this.formative = formative;
        this.summative = summative;
        this.supplementary = supplementary;
    }

    public int getStudentId() {
        return studentId;
    }

    public int getModuleId() {
        return moduleId;
    }

    public String getModuleCode() {
        return moduleCode;
    }

    public String getModuleName() {
        return moduleName;
    }

    public String getExamType() {
        return examType;
    }

    public boolean isReceivedBook() {
        return receivedBook;
    }

    public void setReceivedBook(boolean receivedBook) {
        this.receivedBook = receivedBook;
    }

    public Double getFormative() {
        return formative;
    }

    public void setFormative(Double formative) {
        this.formative = formative;
    }

    public Double getSummative() {
        return summative;
    }

    public void setSummative(Double summative) {
        this.summative = summative;
    }

    public Double getSupplementary() {
        return supplementary;
    }

    public void setSupplementary(Double supplementary) {
        this.supplementary = supplementary;
    }
}
