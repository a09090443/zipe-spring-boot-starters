package com.example.repository

import com.example.model.UserMain
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface UserMainRepository : JpaRepository<UserMain, Int> {

    fun findUserMainByName(@Param("name") name: String): UserMain?
}
