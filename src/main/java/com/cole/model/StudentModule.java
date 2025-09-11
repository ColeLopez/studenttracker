package com.cole.model;

public class StudentModule {
    private int studentId;
    private int moduleId;
    private String moduleCode;
    private String moduleName;
    private boolean receivedBook;
    private Double formative;
    private Double summative;
    private Double supplementary;
    private int passRate = 50; // Default to 50 if not set
    private String signaturePath;
    private String dateIssued; // New field for date issued

    public StudentModule(int studentId, int moduleId, String moduleCode, String moduleName, String examType, boolean receivedBook) {
        this(studentId, moduleId, moduleCode, moduleName, null, null, null, receivedBook, null, null);
    }

    public StudentModule(int studentId, int moduleId, String moduleCode, String moduleName,
                        Double formative, Double summative, Double supplementary,
                        boolean receivedBook, String signaturePath, String dateIssued) {
        this.studentId = studentId;
        this.moduleId = moduleId;
        this.moduleCode = moduleCode;
        this.moduleName = moduleName;
        this.formative = formative;
        this.summative = summative;
        this.supplementary = supplementary;
        this.receivedBook = receivedBook;
        this.signaturePath = signaturePath;
        this.dateIssued = dateIssued;
    }

    public int getPassRate() {
        return passRate;
    }

    public void setPassRate(int passRate) {
        this.passRate = passRate;
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

    public String getSignaturePath() {
        return signaturePath;
    }

    public void setSignaturePath(String signaturePath) {
        this.signaturePath = signaturePath;
    }

    public String getDateIssued() {
        return dateIssued;
    }

    public void setDateIssued(String dateIssued) {
        this.dateIssued = dateIssued;
    }
}
