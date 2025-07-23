package com.cole.model;

import javafx.beans.property.*;

public class StudentToGraduate {
    private final StringProperty studentNumber;
    private final StringProperty firstName;
    private final StringProperty lastName;
    private final StringProperty slpCourse;
    private final StringProperty email;
    private final StringProperty phone;
    private final BooleanProperty transcriptRequested = new SimpleBooleanProperty();

    public StudentToGraduate(String studentNumber, String firstName, String lastName,
                             String slpCourse, String email, String phone, boolean transcriptRequested) {
        this.studentNumber = new SimpleStringProperty(studentNumber);
        this.firstName = new SimpleStringProperty(firstName);
        this.lastName = new SimpleStringProperty(lastName);
        this.slpCourse = new SimpleStringProperty(slpCourse);
        this.email = new SimpleStringProperty(email);
        this.phone = new SimpleStringProperty(phone);
        this.transcriptRequested.set(transcriptRequested);
    }

    public String getStudentNumber() { return studentNumber.get(); }
    public StringProperty studentNumberProperty() { return studentNumber; }

    public String getFirstName() { return firstName.get(); }
    public StringProperty firstNameProperty() { return firstName; }

    public String getLastName() { return lastName.get(); }
    public StringProperty lastNameProperty() { return lastName; }

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
        return getFirstName() + " " + getLastName();
    }
}
