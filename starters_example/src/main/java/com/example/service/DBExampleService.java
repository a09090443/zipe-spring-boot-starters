package com.example.service;

import com.example.model.UserDetail;
import com.example.model.UserMain;

public interface DBExampleService {

  UserMain getUserMainByName(String name);

  UserDetail getUserDetailByName(String name);
}
