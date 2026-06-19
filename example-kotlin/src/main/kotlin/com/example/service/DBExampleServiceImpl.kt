package com.example.service

import com.example.model.UserDetail
import com.example.model.UserMain
import com.example.repository.UserDetailRepository
import com.example.repository.UserMainRepository
import com.zipe.base.annotation.DS
import com.zipe.base.database.DataSourceHolder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * 資料庫範例服務實作類別。
 *
 * 示範 db-spring-boot-starter 提供的動態資料來源切換機制，
 * 包含透過 [DataSourceHolder] 手動切換資料來源，
 * 以及透過 [DS] 注解自動切換的兩種使用方式。
 *
 * @property userMainRepository 使用者主要資料的 JPA Repository，對應預設資料來源。
 * @property userDetailRepository 使用者詳細資料的 JPA Repository，對應由 [DS] 注解指定的資料來源。
 */
@Service
class DBExampleServiceImpl(
    private val userMainRepository: UserMainRepository,
    private val userDetailRepository: UserDetailRepository
) : DBExampleService {

    private val log = LoggerFactory.getLogger(DBExampleServiceImpl::class.java)

    /**
     * 依使用者名稱查詢主要資料，示範以程式碼手動切換資料來源的方式。
     *
     * 方法執行前會先記錄目前的資料來源名稱，
     * 接著明確將資料來源切換為 `"example2"`，再執行查詢。
     *
     * @param name 要查詢的使用者名稱
     * @return 對應的 [UserMain] 實體，若查無資料則回傳 `null`
     */
    override fun getUserMainByName(name: String): UserMain? {
        var dataSourceName = DataSourceHolder.getDataSourceName()
        log.debug("DataSourceName:{}", dataSourceName)
        // 指定 datasource（須為 data-source.properties 中實際存在的資料源名稱）
        DataSourceHolder.setDataSourceName("example2")
        dataSourceName = DataSourceHolder.getDataSourceName()
        log.debug("DataSourceName:{}", dataSourceName)
        return userMainRepository.findUserByName(name)
    }

    /**
     * 依使用者名稱查詢詳細資料，示範以 [DS] 注解自動切換資料來源的方式。
     *
     * `@DS` 注解會在方法執行前由 AOP 攔截，
     * 自動將資料來源切換至注解所設定的目標，無需手動操作 [DataSourceHolder]。
     *
     * @param name 要查詢的使用者名稱
     * @return 對應的 [UserDetail] 實體，若查無資料則回傳 `null`
     */
    @DS("example2")
    override fun getUserDetailByName(name: String): UserDetail? =
        userDetailRepository.findByName(name)
}
