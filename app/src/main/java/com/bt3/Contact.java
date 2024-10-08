package com.bt3;

public class Contact {
    private String name;
    private String phoneNumber;
    private long id; // Thay UUID bằng String để lưu ID thực

    public Contact(String name, String phoneNumber, long contactId) {
        this.name = name;
        this.phoneNumber = phoneNumber;
        this.id = contactId; // Khởi tạo ID thực
    }
    public Contact(String name, String phoneNumber) {
        this.name = name;
        this.phoneNumber = phoneNumber;

    }

    public String getName() {
        return name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public long getId() {
        return id; // Thêm phương thức để lấy ID
    }
}
