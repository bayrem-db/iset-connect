package com.iset.connect.models;

/** Student or administrator account. Field names mirror the JSON API. */
public class User {
    public long id;
    public String first_name;
    public String last_name;
    public String cin;
    public String email;
    public String class_name;
    public String role;
    public String status;
    public String created_at;

    public long getId() { return id; }
    public String getFirstName() { return first_name == null ? "" : first_name; }
    public String getLastName() { return last_name == null ? "" : last_name; }
    public String getFullName() { return (getFirstName() + " " + getLastName()).trim(); }
    public String getCin() { return cin == null ? "" : cin; }
    public String getEmail() { return email == null ? "" : email; }
    public String getClassName() { return class_name == null ? "" : class_name; }
    public String getRole() { return role == null ? "" : role; }
    public String getStatus() { return status == null ? "" : status; }
    public String getCreatedAt() { return created_at == null ? "" : created_at; }
}
