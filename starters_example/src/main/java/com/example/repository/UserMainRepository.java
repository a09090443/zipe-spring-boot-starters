package com.example.repository;

import com.example.model.UserMain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author zipe
 */
@Repository
public interface UserMainRepository extends JpaRepository<UserMain, Integer> {

    UserMain findUserByName(@Param("name") String name);
}
