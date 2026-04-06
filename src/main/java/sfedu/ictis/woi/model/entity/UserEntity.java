package sfedu.ictis.woi.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    @Column(name = "user_password")
    private String password;

    @Column(name = "user_name")
    private String firstName;

    @Column(name = "user_name_last")
    private String lastName;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}