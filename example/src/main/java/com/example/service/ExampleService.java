package com.example.service;

import com.example.model.Gleepf;
import com.example.model.LdapUser;

import java.util.List;

/**
 * @author Gary.Tsai
 */
public interface ExampleService {

    LdapUser findUserByUserId(String userId);

    List<Gleepf> findAll();

    Gleepf findByEE010(String ee010);

    void saveGleepf(Gleepf gleepf);

    void delGleepf(Gleepf gleepf);

    void updateGleepf(Gleepf gleepf);

}
