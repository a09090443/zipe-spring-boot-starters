package com.example.service.impl

import com.example.jdbc.ExampleJdbc
import com.example.model.UserDetail
import com.example.model.UserMain
import com.example.repository.UserDetailRepository
import com.example.repository.UserMainRepository
import com.example.service.ExampleService
import com.zipe.base.annotation.DS
import com.zipe.enums.ResourceEnum
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ExampleServiceImpl : ExampleService {

    @Autowired
    lateinit var userMainRepository: UserMainRepository

    @Autowired
    lateinit var userDetailRepository: UserDetailRepository

    @Autowired
    lateinit var exampleJdbc: ExampleJdbc

    @DS
    override fun findUserByName(name: String) = userMainRepository.findUserMainByName(name)

    @DS("example")
    override fun findUserInfoByName(name: String): UserDetail? = userDetailRepository.findById(name).get()

    @DS
    override fun findUserByJdbc(name: String): List<UserMain>? {
        val resource = ResourceEnum.SQL.getResource("FIND_USER_MAIN_BY_NAME")
        val param = mutableMapOf<String, Any>("name" to name)
        return exampleJdbc.queryForList(resource, param, UserMain::class.java).toList()
    }

    @DS
    @Transactional
    override fun saveOrUpdateUser(userMain: UserMain) = userMainRepository.save(userMain)

    @DS("example")
    @Transactional
    override fun saveOrUpdateInfo(userDetail: UserDetail) = userDetailRepository.save(userDetail)

    @DS
    @Transactional
    override fun deleteUser(userMain: UserMain) = userMainRepository.delete(userMain)

    @DS("example")
    @Transactional
    override fun deleteInfo(userDetail: UserDetail) = userDetailRepository.delete(userDetail)
}
