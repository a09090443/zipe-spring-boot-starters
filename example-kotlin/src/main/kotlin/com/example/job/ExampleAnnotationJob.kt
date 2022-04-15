package com.example.job

import com.example.repository.UserDetailRepository
import com.example.service.ExampleService
import com.zipe.util.log.logger
import com.zipe.util.time.DateTimeUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class ExampleAnnotationJob {

    @Autowired
    lateinit var exampleServiceImpl: ExampleService

    @Scheduled(cron = "0/20 * * * * ?")
    fun exampleJob() {
        logger().info("Job class: ${this::class.simpleName}, 當前執行時間:${DateTimeUtils.getDateNow()}")
        val user = exampleServiceImpl.findUserByName("Andy")
        logger().info("取得使用者:${user.toString()}")
    }
}
