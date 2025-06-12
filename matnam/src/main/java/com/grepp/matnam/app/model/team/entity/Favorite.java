package com.grepp.matnam.app.model.team.entity;

import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.infra.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Favorite extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long favoriteId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "teamId")
    private Team team;

    public Favorite() {
    }

    public Favorite(User user, Team team) {
        this.user = user;
        this.team = team;
    }

    public boolean isActivated() {
        return this.activated;
    }

    public LocalDateTime createdAt() {
        return this.createdAt;
    }


}

