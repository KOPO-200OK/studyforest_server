package com.gongsoop.member.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "MEMBERS")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "NAME", nullable = false, length = 100)
    private String name;

    @Column(name = "BIRTHDATE", nullable = false)
    private LocalDate birthdate;

    @Column(name = "EMAIL", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "PASSWORD", nullable = false, length = 255)
    private String password;

    protected Member() {}

    public Member(String name, LocalDate birthdate, String email, String password) {
        this.name = name;
        this.birthdate = birthdate;
        this.email = email;
        this.password = password;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public LocalDate getBirthdate() { return birthdate; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
}


