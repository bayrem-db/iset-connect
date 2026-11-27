package com.iset.connect.models;

public class Internship {
    public long id;
    public String title;
    public String company_name;
    public String company_address;
    public String domain;
    public String description;
    public String required_skills;
    public String start_date;
    public String end_date;
    public String application_deadline;
    public int total_positions;
    public int available_positions;
    public String status;
    public String created_at;
    public int has_applied;

    public long getId() { return id; }
    public String getTitle() { return title == null ? "" : title; }
    public String getCompanyName() { return company_name == null ? "" : company_name; }
    public String getCompanyAddress() { return company_address == null ? "" : company_address; }
    public String getDomain() { return domain == null ? "" : domain; }
    public String getDescription() { return description == null ? "" : description; }
    public String getRequiredSkills() { return required_skills == null ? "" : required_skills; }
    public String getStartDate() { return start_date == null ? "" : start_date; }
    public String getEndDate() { return end_date == null ? "" : end_date; }
    public String getApplicationDeadline() { return application_deadline == null ? "" : application_deadline; }
    public int getTotalPositions() { return total_positions; }
    public int getAvailablePositions() { return available_positions; }
    public String getStatus() { return status == null ? "" : status; }
    public String getCreatedAt() { return created_at == null ? "" : created_at; }
    public boolean hasApplied() { return has_applied == 1; }
}
