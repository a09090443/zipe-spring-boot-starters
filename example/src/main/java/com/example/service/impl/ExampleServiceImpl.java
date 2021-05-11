package com.example.service.impl;

import com.example.jdbc.ExampleJdbc;
import com.example.model.UserDetail;
import com.example.model.UserMain;
import com.example.repository.UserDetailRepository;
import com.example.repository.UserMainRepository;
import com.example.service.ExampleService;
import com.zipe.base.annotation.DS;
import com.zipe.enums.ResourceEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Gary.Tsai
 */
@Service
public class ExampleServiceImpl implements ExampleService {

    private final UserMainRepository userMainRepository;

    private final UserDetailRepository userDetailRepository;

    private final ExampleJdbc exampleJdbc;

    @Autowired
    public ExampleServiceImpl(UserMainRepository userMainRepository,
                              UserDetailRepository userDetailRepository,
                              ExampleJdbc exampleJdbc) {
        this.userMainRepository = userMainRepository;
        this.userDetailRepository = userDetailRepository;
        this.exampleJdbc = exampleJdbc;
    }

    @Override
    @DS
    public UserMain findUserById(Integer id) {
        return userMainRepository.findById(id).get();
    }

    @Override
    @DS
    public UserMain findUserByName(String name) {
        return userMainRepository.findUserByName(name);
    }

    @Override
    @DS
    public List<UserMain> findAll() {
        return userMainRepository.findAll();
    }

    @Override
    @DS
    @Transactional
    public UserMain saveOrUpdateUser(UserMain userMain) {
        return userMainRepository.save(userMain);
    }

    @Override
    @DS
    @Transactional
    public void deleteUser(UserMain userMain) {
        userMainRepository.delete(userMain);
    }

    @Override
    @DS("example")
    public UserDetail findByUserName(String name) {
        return userDetailRepository.findById(name).get();
    }

    @Override
    @DS("example")
    @Transactional
    public UserDetail saveOrUpdateInfo(UserDetail userDetail) {
        return userDetailRepository.save(userDetail);
    }

    @Override
    @DS("example")
    @Transactional
    public void deleteInfo(UserDetail userDetail) {
        userDetailRepository.delete(userDetail);
    }

    @Override
    @DS
    public List<UserMain> findUserByJdbc(String name){
        ResourceEnum resource = ResourceEnum.SQL.getResource("FIND_USER_MAIN_BY_NAME");
        Map<String, Object> params = new HashMap<>();
        params.put("name", name);
        List<UserMain> userMains = exampleJdbc.queryForList(resource, params, UserMain.class);
        return userMains;
    }
}
