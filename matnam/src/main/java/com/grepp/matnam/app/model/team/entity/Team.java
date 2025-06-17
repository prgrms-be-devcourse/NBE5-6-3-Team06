package com.grepp.matnam.app.model.team.entity;

import com.grepp.matnam.app.model.restaurant.entity.Restaurant;
import com.grepp.matnam.app.model.team.code.Status;
import com.grepp.matnam.app.model.user.entity.User;
import com.grepp.matnam.infra.entity.BaseEntity;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
public class Team extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long teamId;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "userId")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurantId")
    private Restaurant restaurant;

    @OneToMany(cascade = CascadeType.PERSIST)
    @JoinColumn(name = "teamId")
    private List<Participant> participants;

    @OneToMany(mappedBy = "team", cascade = CascadeType.ALL)
    private List<Favorite> favorites;
    
    private String restaurantAddress;
    private String category;

    private String teamTitle;

    private String teamDetails;

    private LocalDateTime teamDate;

    private Integer maxPeople;

    private Integer nowPeople;

    @Enumerated(EnumType.STRING)
    private Status status;

    private String imageUrl;

    private String restaurantName;

    private Double latitude;

    private Double longitude;

    @Column(name = "view_count", nullable = false)
    private Long viewCount = 0L;

    public boolean isActivated() {
        return this.activated;
    }

    public LocalDateTime createdAt() {
        return this.createdAt;
    }

    @Transient
    private long favoriteCount;

}