package com.example.repository;

import com.example.model.Info;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author zipe
 */
@Repository
public interface InfoRepository extends JpaRepository<Info, Integer> {
}
