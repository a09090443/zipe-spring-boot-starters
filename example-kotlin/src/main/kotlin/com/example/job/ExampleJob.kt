package com.example.job

import com.example.service.ExampleService
import com.zipe.quartz.job.QuartzJobFactory
import com.zipe.util.log.logger
import com.zipe.util.time.DateTimeUtils
import org.quartz.JobExecutionContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class ExampleJob: QuartzJobFactory() {

    @Autowired
    lateinit var exampleServiceImpl: ExampleService

    override fun executeJob(jobExecutionContext: JobExecutionContext?) {
        logger().info("Job class: ${this::class.simpleName}, 當前執行時間:${DateTimeUtils.dateTime}")
        val user = exampleServiceImpl.findUserByName("Tom")
        logger().info("取得使用者:${user.toString()}")
    }

}
