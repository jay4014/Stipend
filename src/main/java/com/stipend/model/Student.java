package com.stipend.model;

import java.time.LocalDate;

/**
 * ENCAPSULATION: Student wraps the account holder's identity.
 * The password hash and salt are stored in the DB; they are never
 * surfaced by this class (no getPasswordHash() / getSalt()).
 *
 * Methods addStudent() and updateStudent() are performed by AuthService
 * using the repository pattern — Student is a pure data carrier.
 */
public class Student {

    // All fields private — ENCAPSULATION
    private int    studentId;
    private String name;
    private String course;
    private String username;
    private LocalDate createdAt;
    private byte[] profileImage;

    // ---------------------------------------------------------------
    // Constructor
    // ---------------------------------------------------------------
    public Student(int studentId, String name,
                   String course, String username) {
        setStudentId(studentId);
        setName(name);
        setCourse(course);
        setUsername(username);
        this.createdAt = LocalDate.now();
    }

    // ---------------------------------------------------------------
    // Business helpers
    // ---------------------------------------------------------------

    /**
     * Returns the student's initials for the avatar in the sidebar.
     * Example: "Juan dela Cruz" → "JD"
     */
    public String getInitials() {
        if (name == null || name.isBlank()) return "?";
        String[] parts = name.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase();
    }

    /** First name only, for the greeting label. */
    public String getFirstName() {
        if (name == null || name.isBlank()) return "Student";
        return name.trim().split("\\s+")[0];
    }

    // ---------------------------------------------------------------
    // Getters
    // ---------------------------------------------------------------
    public int       getStudentId() { return studentId; }
    public String    getName()      { return name;       }
    public String    getCourse()    { return course;     }
    public String    getUsername()  { return username;   }
    public LocalDate getCreatedAt() { return createdAt;  }
    public byte[] getProfileImage() {
        return profileImage == null ? null : profileImage.clone();
    }

    // ---------------------------------------------------------------
    // Setters with validation — ENCAPSULATION
    // ---------------------------------------------------------------
    public void setStudentId(int id) {
        if (id < 0) throw new IllegalArgumentException("Student ID must be >= 0.");
        this.studentId = id;
    }

    public void setName(String name) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("Name must not be blank.");
        this.name = name.trim();
    }

    public void setCourse(String course) {
        if (course == null || course.isBlank())
            throw new IllegalArgumentException("Course must not be blank.");
        this.course = course.trim();
    }

    public void setUsername(String username) {
        if (username == null || username.isBlank())
            throw new IllegalArgumentException("Username must not be blank.");
        this.username = username.trim();
    }

    public void setProfileImage(byte[] profileImage) {
        this.profileImage = profileImage == null ? null : profileImage.clone();
    }

    @Override
    public String toString() {
        return "Student{id=" + studentId + ", name='" + name
             + "', course='" + course + "', username='" + username + "'}";
    }
}
