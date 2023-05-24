package com.example.repository;

import com.example.model.UserDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author zipe
 */
@Repository
public interface UserDetailRepository extends JpaRepository<UserDetail, String> {

  UserDetail findByName(String name);

  UserDetail findByGender(String gender);
}
