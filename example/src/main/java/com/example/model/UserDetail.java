package com.example.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Data;


/**
 * @author Gary.Tsai
 */
@Data
@Entity
public class UserDetail {

    @Id
    private String name;

    private String gender;
}
