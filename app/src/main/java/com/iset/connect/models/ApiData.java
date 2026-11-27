package com.iset.connect.models;

import java.util.List;

/** Typed wrappers for the "data" object of each API response. */
public final class ApiData {
    private ApiData() { }

    public static class LoginData {
        public String token;
        public User user;
        public String status;
        public String getToken() { return token == null ? "" : token; }
        public User getUser() { return user; }
        public String getStatus() { return status == null ? "" : status; }
    }

    public static class UserData {
        public User user;
        public User getUser() { return user; }
    }

    public static class StudentListData {
        public List<User> students;
        public List<User> getStudents() { return students; }
    }

    public static class StudentDetailsData {
        public User student;
        public List<Loan> loans;
        public List<Application> applications;
    }

    public static class MaterialData {
        public Material material;
        public Material getMaterial() { return material; }
    }

    public static class MaterialListData {
        public List<Material> materials;
        public List<Material> getMaterials() { return materials; }
    }

    public static class LoanData {
        public Loan loan;
        public Loan getLoan() { return loan; }
    }

    public static class LoanListData {
        public List<Loan> loans;
        public List<Loan> getLoans() { return loans; }
    }

    public static class InternshipData {
        public Internship internship;
        public Internship getInternship() { return internship; }
    }

    public static class InternshipListData {
        public List<Internship> internships;
        public List<Internship> getInternships() { return internships; }
    }

    public static class ApplicationData {
        public Application application;
        public Application getApplication() { return application; }
    }

    public static class ApplicationListData {
        public List<Application> applications;
        public List<Application> getApplications() { return applications; }
    }

    public static class NotificationListData {
        public List<Notification> notifications;
        public int unread_count;
        public List<Notification> getNotifications() { return notifications; }
        public int getUnreadCount() { return unread_count; }
    }

    public static class AdminStats {
        public int total_students;
        public int pending_students;
        public int approved_students;
        public int total_materials;
        public int available_materials;
        public int borrowed_materials;
        public int total_loans;
        public int pending_loans;
        public int total_internships;
        public int published_internships;
        public int pending_applications;
        public int unread_notifications;
    }

    public static class StudentStats {
        public int available_materials;
        public int my_loans;
        public int my_borrowed;
        public int available_internships;
        public int my_applications;
        public int unread_notifications;
        public User user;
        public User getUser() { return user; }
    }
}
