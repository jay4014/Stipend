package com.stipend;

public class Student {

    private int studentId;
    private String name;
    private String course;
    private String username;

    public Student(int studentId, String name,
                   String course, String username) {
        this.studentId = studentId;
        this.name = name;
        this.course = course;
        this.username = username;
    }

    public int getStudentId() {
        return studentId;
    }

    public String getName() {
        return name;
    }

    public String getCourse() {
        return course;
    }

    public String getUsername() {
        return username;
    }

    public void displayStudentInfo() {
        System.out.println("=== STUDENT INFORMATION ===");
        System.out.println("Student ID: " + studentId);
        System.out.println("Name: " + name);
        System.out.println("Course: " + course);
        System.out.println("Username: " + username);
    }
}
