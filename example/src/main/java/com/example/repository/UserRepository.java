package com.example.repository;

import com.example.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author zipe
 */
@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    User findUserByName(@Param("name") String name);
}
