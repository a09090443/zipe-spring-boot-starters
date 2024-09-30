package com.example.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

/**
 * @author Gary.Tsai
 */
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name="user_main")
public class UserMain {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        UserMain userMain = (UserMain) o;
        return getId() != null && Objects.equals(getId(), userMain.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
