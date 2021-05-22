package com.example.service

import com.example.model.UserDetail
import com.example.model.UserMain

interface ExampleService {
    fun findUserByName(name: String): UserMain?

    fun findUserInfoByName(name: String): UserDetail?

    fun findUserByJdbc(name: String): List<UserMain>?

    fun saveOrUpdateUser(userMain: UserMain): UserMain

    fun saveOrUpdateInfo(userDetail: UserDetail): UserDetail

    fun deleteUser(userMain: UserMain)

    fun deleteInfo(userDetail: UserDetail)
}
