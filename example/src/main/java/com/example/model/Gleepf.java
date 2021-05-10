package com.example.model;

import lombok.Data;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * @author : Gary Tsai
 **/
@Data
@Entity
@Table
public class Gleepf implements Serializable {

    @Id
    private String ee010 = "";
    private BigDecimal ee020;
    private String ee030 = "";
    private String ee040 = "";
    private String ee050 = "";
    private String ee060 = "";
    private String ee070 = "";
    private String ee080 = "";
    private String ee090 = "";
    private String ee100 = "";
    private String ee110 = "";
    private String ee120 = "";
    private String ee130 = "";
    private String ee140 = "";
    private BigDecimal ee150;
    private String ee160 = "";
    private BigDecimal ee170;
    private String ee180 = "";
    private String ee181 = "";
    private String eeMark = "";
    private BigDecimal eeDate;
    private BigDecimal eeTime;
    private String eeUser = "";
}
