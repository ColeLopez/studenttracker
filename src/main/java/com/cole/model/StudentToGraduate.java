package com.cole.model;

import javafx.beans.property.*;

public class StudentToGraduate {
    private final StringProperty studentNumber;
    private final StringProperty firstName;
    private final StringProperty secondName;
    private final StringProperty lastName;
    private final StringProperty idNumber;
    private final StringProperty slpCourse;
    private final StringProperty email;
    private final StringProperty phone;
    private final StringProperty branch;
    private final BooleanProperty transcriptRequested = new SimpleBooleanProperty();

    public StudentToGraduate(String studentNumber, String firstName, String secondName, String lastName,
                             String idNumber, String slpCourse, String email, String phone, String branch, boolean transcriptRequested) {
        this.studentNumber = new SimpleStringProperty(studentNumber);
        this.firstName = new SimpleStringProperty(firstName);
        this.secondName = new SimpleStringProperty(secondName);
        this.lastName = new SimpleStringProperty(lastName);
        this.idNumber = new SimpleStringProperty(idNumber);
        this.slpCourse = new SimpleStringProperty(slpCourse);
        this.email = new SimpleStringProperty(email);
        this.phone = new SimpleStringProperty(phone);
        this.branch = new SimpleStringProperty(branch);
        this.transcriptRequested.set(transcriptRequested);
    }

    public String getStudentNumber() { return studentNumber.get(); }
    public StringProperty studentNumberProperty() { return studentNumber; }

    public String getFirstName() { return firstName.get(); }
    public StringProperty firstNameProperty() { return firstName; }

    public String getSecondName() { return secondName.get(); }
    public StringProperty secondNameProperty() { return secondName; }

    public String getLastName() { return lastName.get(); }
    public StringProperty lastNameProperty() { return lastName; }

    public String getIdNumber() { return idNumber.get(); }
    public StringProperty idNumberProperty() { return idNumber; }

    public String getBranch() { return branch.get(); }
    public StringProperty branchProperty() { return branch; }

    public String getSlpCourse() { return slpCourse.get(); }
    public StringProperty slpCourseProperty() { return slpCourse; }

    public String getEmail() { return email.get(); }
    public StringProperty emailProperty() { return email; }

    public String getPhone() { return phone.get(); }
    public StringProperty phoneProperty() { return phone; }

    public BooleanProperty transcriptRequestedProperty() { return transcriptRequested; }
    public boolean isTranscriptRequested() { return transcriptRequested.get(); }
    public void setTranscriptRequested(boolean value) { transcriptRequested.set(value); }

    // For TableView "Student Name" column
    public String getFullName() {
        return getFirstName() + " " + getSecondName() + " " + getLastName();
    }
}
