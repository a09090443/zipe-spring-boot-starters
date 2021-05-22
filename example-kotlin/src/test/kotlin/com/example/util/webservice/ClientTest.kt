package com.example.util.webservice

import com.zipe.util.WebServiceClientUtil
import com.zipe.util.log.logger
import org.junit.jupiter.api.Test

class ClientTest {

    @Test
    fun `call web service and getting callback`(){
        val wsdlUrl = "http://localhost:8080/example/webservice/example?wsdl"
        val methodName = "helloWorld"
        WebServiceClientUtil(wsdlUrl, methodName, arrayOf("Zipe")).run {
            val callback = this.invoke()
            logger().info(callback.toString())
        }
    }
}
