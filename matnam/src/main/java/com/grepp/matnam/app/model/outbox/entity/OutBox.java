package com.grepp.matnam.app.model.outbox.entity;

import com.grepp.matnam.infra.entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name="OutBox")
@NoArgsConstructor
@Getter
public class OutBox extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String event;

    private String nickName;

    private String email;

    private String emailCode;

    public OutBox(String event, String email, String nickName, String emailCode) {
        this.event = event;
        this.email = email;
        this.nickName = nickName;
        this.emailCode = emailCode;
    }

}
