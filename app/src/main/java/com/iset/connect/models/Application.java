package com.iset.connect.models;

public class Application {
    public long id;
    public long student_id;
    public String student_name;
    public String cin;
    public String class_name;
    public long internship_id;
    public String internship_title;
    public String company_name;
    public String motivation_message;
    public String application_date;
    public String status;

    public long getId() { return id; }
    public long getStudentId() { return student_id; }
    public String getStudentName() { return student_name == null ? "" : student_name; }
    public String getCin() { return cin == null ? "" : cin; }
    public String getClassName() { return class_name == null ? "" : class_name; }
    public long getInternshipId() { return internship_id; }
    public String getInternshipTitle() { return internship_title == null ? "" : internship_title; }
    public String getCompanyName() { return company_name == null ? "" : company_name; }
    public String getMotivationMessage() { return motivation_message == null ? "" : motivation_message; }
    public String getApplicationDate() { return application_date == null ? "" : application_date; }
    public String getStatus() { return status == null ? "" : status; }
}
