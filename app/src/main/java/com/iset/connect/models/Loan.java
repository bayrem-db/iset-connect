package com.iset.connect.models;

public class Loan {
    public long id;
    public long student_id;
    public String student_name;
    public long material_id;
    public String material_name;
    public String material_category;
    public int quantity;
    public String request_date;
    public String borrowing_date;
    public String expected_return_date;
    public String actual_return_date;
    public String status;

    public long getId() { return id; }
    public long getStudentId() { return student_id; }
    public String getStudentName() { return student_name == null ? "" : student_name; }
    public long getMaterialId() { return material_id; }
    public String getMaterialName() { return material_name == null ? "" : material_name; }
    public String getCategory() { return material_category == null ? "" : material_category; }
    public int getQuantity() { return quantity; }
    public String getRequestDate() { return request_date == null ? "" : request_date; }
    public String getBorrowingDate() { return borrowing_date == null ? "" : borrowing_date; }
    public String getExpectedReturnDate() { return expected_return_date == null ? "" : expected_return_date; }
    public String getActualReturnDate() { return actual_return_date == null ? "" : actual_return_date; }
    public String getStatus() { return status == null ? "" : status; }
}
