package com.example.repository;

import com.example.entity.InfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InfoRepository extends JpaRepository<InfoEntity, String> {

    List<InfoEntity> findAll();
}
