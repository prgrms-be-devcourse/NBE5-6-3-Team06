package com.grepp.matnam.app.model.user.entity;


import com.grepp.matnam.app.model.user.code.Role;
import com.grepp.matnam.app.model.team.entity.Participant;
import com.grepp.matnam.app.model.user.code.Gender;
import com.grepp.matnam.app.model.user.code.Status;
import com.grepp.matnam.infra.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString(exclude = {"preference", "maps", "participants"})
public class User extends BaseEntity {

    @Id
    @Column(nullable = false, unique = true)
    private String userId;

    private String password;

    private String email;

    private String address;

    private String nickname;

    private int age;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private float temperature;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name="userId")
    private Preference preference;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private List<Map> maps;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "userId")
    private List<Participant> participants;

    @Enumerated(EnumType.STRING)
    private Status status = Status.ACTIVE;

    public boolean isActivated() {
        return this.activated;
    }

    public LocalDateTime createdAt() {
        return this.createdAt;
    }

    private LocalDate dueDate;

    private Integer suspendDuration;

    private String dueReason;
}