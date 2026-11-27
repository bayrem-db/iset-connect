package com.iset.connect.models;

public class Material {
    public long id;
    public String name;
    public String description;
    public String category;
    public int total_quantity;
    public int available_quantity;
    public String image_url;
    public String created_at;
    public String updated_at;

    public long getId() { return id; }
    public String getName() { return name == null ? "" : name; }
    public String getDescription() { return description == null ? "" : description; }
    public String getCategory() { return category == null ? "" : category; }
    public int getTotalQuantity() { return total_quantity; }
    public int getAvailableQuantity() { return available_quantity; }
    public String getImageUrl() { return image_url == null ? "" : image_url; }
    public int getBorrowedQuantity() { return total_quantity - available_quantity; }
    public String getCreatedAt() { return created_at == null ? "" : created_at; }
}
