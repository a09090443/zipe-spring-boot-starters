package com.example.service

import com.example.base.TestBase
import com.example.model.UserDetail
import com.example.model.UserMain
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Order

class ExampleServiceTest(
    var exampleServiceImpl: ExampleService
) : TestBase() {

    companion object {
        private const val USERNAME = "Gary"
    }

    @BeforeAll
    fun `insert new user`() {
        val main = UserMain(name = USERNAME).run {
            exampleServiceImpl.saveOrUpdateUser(this)
        }

        UserDetail().run {
            this.name = main.name
            this.gender = "M"
            this
        }.run {
            exampleServiceImpl.saveOrUpdateInfo(this)
        }
    }

    @AfterAll
    fun `delete user`() {
        val main = exampleServiceImpl.findUserByName(USERNAME)
        exampleServiceImpl.deleteUser(main!!)
        val detail = exampleServiceImpl.findUserInfoByName(main.name)
        exampleServiceImpl.deleteInfo(detail!!)
    }

    @Test
    @Order(1)
    fun `find user by username`() {
        val main = exampleServiceImpl.findUserByName(USERNAME)
        Assertions.assertNotNull(main)
    }

    @Test
    @Order(2)
    fun `update user`() {
        val main = exampleServiceImpl.findUserByName(USERNAME) ?: throw Exception()

        val gender = "F"
        exampleServiceImpl.findUserInfoByName(main.name)?.let {
            it.gender = gender
            exampleServiceImpl.saveOrUpdateInfo(it)
        }

        val detail = exampleServiceImpl.findUserInfoByName(main.name)
        Assertions.assertEquals(gender, detail?.gender)
    }

    @Test
    @Order(3)
    fun `find user by jdbc`() {
        val main = exampleServiceImpl.findUserByJdbc(USERNAME)
        Assertions.assertNotNull(main)
    }
}
