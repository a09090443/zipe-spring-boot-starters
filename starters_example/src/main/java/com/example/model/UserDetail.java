package com.example.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.Objects;
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
@Table(name="user_detail")
public class UserDetail {

    @Id
    private String name;

    private String gender;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        UserDetail that = (UserDetail) o;
        return getName() != null && Objects.equals(getName(), that.getName());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
