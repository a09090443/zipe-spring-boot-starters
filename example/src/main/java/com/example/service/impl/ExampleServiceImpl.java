package com.example.service.impl;

import com.example.model.Info;
import com.example.model.User;
import com.example.repository.InfoRepository;
import com.example.repository.UserRepository;
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

    private final UserRepository userRepository;

    private final InfoRepository infoRepository;

    @Autowired
    public ExampleServiceImpl(UserRepository userRepository,
                              InfoRepository infoRepository) {
        this.userRepository = userRepository;
        this.infoRepository = infoRepository;
    }

    @Override
    @DS
    public User findUserById(Integer id) {
        return userRepository.findById(id).get();
    }

    @Override
    @DS
    public User findUserByName(String name) {
        return userRepository.findUserByName(name);
    }

    @Override
    @DS
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @DS
    @Transactional
    public User saveOrUpdateUser(User user) {
        return userRepository.save(user);
    }

    @Override
    @DS
    @Transactional
    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    @Override
    @DS("example")
    public Info findByUserId(Integer userId) {
        return infoRepository.findById(userId).get();
    }

    @Override
    @DS("example")
    @Transactional
    public Info saveOrUpdateInfo(Info info) {
        return infoRepository.save(info);
    }

    @Override
    @DS("example")
    @Transactional
    public void deleteInfo(Info info) {
        infoRepository.delete(info);
    }

}
