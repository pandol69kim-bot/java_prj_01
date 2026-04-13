package com.example.app.domain.user;

import com.example.app.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_users_username", columnList = "username", unique = true),
    @Index(name = "idx_users_email", columnList = "email", unique = true)
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "password")
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    public enum Role {
        ROLE_ADMIN, ROLE_USER
    }

    public static User of(String username, String email, String encodedPassword, Role role) {
        User user = new User();
        user.username = username;
        user.email = email;
        user.password = encodedPassword;
        user.role = role;
        return user;
    }

    public User withEncodedPassword(String encodedPassword) {
        User updated = new User();
        updated.username = this.username;
        updated.email = this.email;
        updated.password = encodedPassword;
        updated.role = this.role;
        updated.enabled = this.enabled;
        return updated;
    }

    public User disable() {
        User updated = new User();
        updated.username = this.username;
        updated.email = this.email;
        updated.password = this.password;
        updated.role = this.role;
        updated.enabled = false;
        return updated;
    }
}
