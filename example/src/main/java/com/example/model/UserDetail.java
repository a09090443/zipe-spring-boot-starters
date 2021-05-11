package com.example.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

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
