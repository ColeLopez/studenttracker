package com.cole.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;

public class Student {

    private final SimpleIntegerProperty id;
    private final SimpleStringProperty studentNumber;
    private final SimpleStringProperty firstName;
    private final SimpleStringProperty lastName;
    private final SimpleStringProperty email;
    private final SimpleStringProperty phoneNumber;
    private final SimpleStringProperty slp;
    private final SimpleStringProperty status;
    private final SimpleStringProperty enrollmentDate;

    public Student(int id, String studentNumber, String firstName, String lastName, String email, String phoneNumber, String slp, String status, String enrollmentDate) {
        this.id = new SimpleIntegerProperty(id);
        this.studentNumber = new SimpleStringProperty(studentNumber);
        this.firstName = new SimpleStringProperty(firstName);
        this.lastName = new SimpleStringProperty(lastName);
        this.email = new SimpleStringProperty(email);
        this.phoneNumber = new SimpleStringProperty(phoneNumber);
        this.slp = new SimpleStringProperty(slp);
        this.status = new SimpleStringProperty(status);
        this.enrollmentDate = new SimpleStringProperty(enrollmentDate);
    }

    public int getId(){
        return id.get();
    }

    public String getStudentNumber() {
        return studentNumber.get();
    }

    public String getFirstName() {
        return firstName.get();
    }

    public String getLastName() {
        return lastName.get();
    }

    public String getEmail() {
        return email.get();
    }

    public String getPhoneNumber() {
        return phoneNumber.get();
    }

    public String getSlp() {
        return slp.get();
    }

    public String getStatus() {
        return status.get();
    }

    public String getEnrollmentDate() {
        return enrollmentDate.get();
    }

    public SimpleStringProperty studentNumberProperty() {
        return studentNumber;
    }

    public SimpleStringProperty firstNameProperty() {
        return firstName;
    }

    public SimpleStringProperty lastNameProperty() {
        return lastName;
    }

    public SimpleStringProperty emailProperty() {
        return email;
    }

    public SimpleStringProperty phoneNumberProperty() {
        return phoneNumber;
    }

    public SimpleStringProperty slpProperty() {
        return slp;
    }

    public SimpleStringProperty statusProperty() {
        return status;
    }

    public SimpleStringProperty enrollmentDateProperty() {
        return enrollmentDate;
    }

}
