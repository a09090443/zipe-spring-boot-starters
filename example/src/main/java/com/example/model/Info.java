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
public class Info {

    @Id
    private Integer userId;

    private String gender;
}
