package com.example.repository;

import com.example.entity.AmlWhiteListEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmlWhiteListRepository extends JpaRepository<AmlWhiteListEntity, String> {

    List<AmlWhiteListEntity> findAll();
}
