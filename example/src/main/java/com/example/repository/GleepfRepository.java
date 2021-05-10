package com.example.repository;

import com.example.model.Gleepf;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/12/2 上午 09:58
 **/
@Repository
public interface GleepfRepository extends JpaRepository<Gleepf, Long> {

    Gleepf findByEe010(@Param("ee010") String ee010);

    @Modifying
    @Query("UPDATE Gleepf gl SET gl.ee180 = :status WHERE gl.ee010 = :number")
    void updateC0401Status(@Param("number") String number, @Param("status") String status);

    @Modifying
    @Query("UPDATE Gleepf gl SET gl.ee181 = :status WHERE gl.ee010 = :number")
    void updateC0501Status(@Param("number") String number, @Param("status") String status);

    @Modifying
    @Query("UPDATE Gleepf gl SET gl.ee180 = :createStatus, gl.ee181 = :delStatus WHERE gl.ee010 = :number")
    void updateC0701Status(@Param("number") String number, @Param("createStatus") String createStatus, @Param("delStatus") String delStatus);
}
