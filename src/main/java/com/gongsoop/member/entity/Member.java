package com.gongsoop.member.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "SF_USER")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "USER_ID")
    private Long id;

    @Column(
            name = "NAME",
            nullable = false,
            length = 100
    )
    private String name;

    @Column(
            name = "NICKNAME",
            nullable = false,
            length = 16
    )
    private String nickname;

    @Column(
            name = "BIRTHDATE",
            nullable = false
    )
    private LocalDate birthdate;

    @Column(
            name = "EMAIL",
            nullable = false,
            unique = true,
            length = 255
    )
    private String email;

    @Column(
            name = "PASSWORD",
            nullable = false,
            length = 255
    )
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "USER_ROLE",
            nullable = false,
            length = 20
    )
    private MemberRole userRole;

    @Column(
            name = "IS_DELETED",
            nullable = false
    )
    private boolean isDeleted;

    @Column(
            name = "CHARACTER_ID",
            nullable = false
    )
    private Integer characterId;

    protected Member() {
    }

    public Member(
            String name,
            String nickname,
            LocalDate birthdate,
            String email,
            String password,
            Integer characterId
    ) {
        this.name = name;
        this.nickname = nickname;
        this.birthdate = birthdate;
        this.email = email;
        this.password = password;
        this.userRole = MemberRole.USER;
        this.isDeleted = false;
        this.characterId = characterId;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getNickname() {
        return nickname;
    }

    public LocalDate getBirthdate() {
        return birthdate;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public MemberRole getUserRole() {
        return userRole;
    }

    public boolean isDeleted() {
        return isDeleted;
    }

    public Integer getCharacterId() {
        return characterId;
    }

    public void updateCharacter(
            Integer characterId
    ) {
        this.characterId = characterId;
    }

    public void updateProfile(
            String nickname,
            Integer characterId
    ) {
        this.nickname = nickname;
        this.characterId = characterId;
    }

    public void delete() {
        this.isDeleted = true;
    }

    public void updatePassword(
            String encodedPassword
    ) {
        this.password = encodedPassword;
    }
}