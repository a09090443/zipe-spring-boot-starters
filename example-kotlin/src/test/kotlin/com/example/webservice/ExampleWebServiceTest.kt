package com.example.webservice

import com.zipe.model.User
import com.zipe.util.WebServiceClientUtil
import io.kotest.core.spec.style.FunSpec
import org.apache.cxf.jaxws.JaxWsProxyFactoryBean
import org.apache.cxf.jaxws.endpoint.dynamic.JaxWsDynamicClientFactory
import org.slf4j.LoggerFactory
import javax.xml.namespace.QName

/**
 * CXF WebService 客戶端測試：以三種方式（代理工廠、ClientUtil 工具、動態客戶端）呼叫
 * 已啟動的 `example` WebService，驗證可取得使用者資料。
 *
 * 需服務端於 `http://localhost:8080/example/webservice/example?wsdl` 運行，
 * 與 Java 來源相同採容錯處理（呼叫失敗僅印出例外，不讓測試直接失敗）。
 */
class ExampleWebServiceTest : FunSpec({

    val log = LoggerFactory.getLogger(ExampleWebServiceTest::class.java)
    val webServiceUrl = "http://localhost:8080/example/webservice/example?wsdl"

    test("getUserByProxy") {
        try {
            // 代理工廠
            val jaxWsProxyFactoryBean = JaxWsProxyFactoryBean()
            // 設置代理地址
            jaxWsProxyFactoryBean.address = webServiceUrl
            // 設置接口類型
            jaxWsProxyFactoryBean.serviceClass = ExampleWebService::class.java
            // 創建一個代理接口實現
            val us = jaxWsProxyFactoryBean.create() as ExampleWebService
            // 數據准備
            val userId = "02"
            // 調用代理接口的方法調用並返回結果
            val result: User? = us.getUser(userId)
            log.info("返回結果:{}", result)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    test("getUserByClientUtil") {
        val clientUtil = WebServiceClientUtil(
            webServiceUrl, "getUser", arrayOf<Any>("01"),
        )
        val result = clientUtil.invoke()
        log.info("返回結果:{}", result[0])
    }

    test("getUserNameByDynamicClient") {
        try {
            // 代理工廠
            val jaxWsDynamicClientFactory = JaxWsDynamicClientFactory.newInstance()
            // 設置代理地址
            val client = jaxWsDynamicClientFactory.createClient(webServiceUrl)
            // 設置targetNamespace 和 methodName
            val qname = QName("http://service.example.com", "getUserName")
            val result = client.invoke(qname, "01")
            log.info("返回結果:{}", result[0])
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
})
