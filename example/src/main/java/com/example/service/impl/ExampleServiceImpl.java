package com.example.service.impl;

import com.example.model.Gleepf;
import com.example.model.LdapUser;
import com.example.repository.GleepfRepository;
import com.example.repository.LdapUserRepository;
import com.example.service.ExampleService;
import com.zipe.base.annotation.DS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @author Gary.Tsai
 */
@Service
public class ExampleServiceImpl implements ExampleService {

    private final LdapUserRepository ldapUserRepository;

    private final GleepfRepository gleepfRepository;

    @Autowired
    public ExampleServiceImpl(LdapUserRepository ldapUserRepository,
                              GleepfRepository gleepfRepository){
        this.ldapUserRepository = ldapUserRepository;
        this.gleepfRepository = gleepfRepository;
    }

    @Override
    @DS
    public LdapUser findUserByUserId(String userId) {
        return ldapUserRepository.findByUserId(userId);
    }

    @Override
    @DS("example")
    public List<Gleepf> findAll() {
        return gleepfRepository.findAll();
    }

    @Override
    @DS("example")
    public Gleepf findByEE010(String ee010) {
        return gleepfRepository.findByEe010(ee010);
    }

    @Override
    @Transactional
    @DS("example")
    public void saveGleepf(Gleepf gleepf) {
        gleepfRepository.save(gleepf);
    }

    @Override
    @Transactional
    @DS("example")
    public void delGleepf(Gleepf gleepf) {
        gleepfRepository.delete(gleepf);
    }

    @Override
    @Transactional
    @DS("example")
    public void updateGleepf(Gleepf gleepf) {
        gleepfRepository.save(gleepf);
    }
}
