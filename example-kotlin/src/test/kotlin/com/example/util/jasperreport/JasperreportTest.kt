package com.example.util.jasperreport

import com.zipe.util.doc.JasperReportUtil
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import net.sf.jasperreports.engine.JRDataSource
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperExportManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.JasperReport
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import org.slf4j.LoggerFactory
import org.springframework.util.ResourceUtils
import java.io.File
import java.util.LinkedList

/**
 * JasperReports 報表產出測試。
 *
 * 分別示範透過 [JasperReportUtil] 工具方法，以及直接使用 JasperReports 原生 API
 * （compile → fill → export）產生 PDF；PDF 輸出至 Kotest 提供的暫存目錄，
 * 避免硬編路徑並可跨平台執行。
 */
class JasperreportTest : FunSpec({

    val log = LoggerFactory.getLogger(JasperreportTest::class.java)

    // PDF 輸出至 Kotest 提供的暫存目錄，spec 結束後自動清除
    val tempDir = tempdir()

    test("exportPdfUserUtil") {
        log.info("generating pdf file using util...")

        val output = File(tempDir, "SimpleReporter.pdf")
        val simpleBeanMaker = SimpleBeanMaker()
        val simpleBeanList = simpleBeanMaker.getDataBeanList()
        val parameters = HashMap<String, Any>()
        val beanColDataSource = JRBeanCollectionDataSource(simpleBeanList)
        JasperReportUtil.exportPdfFile(
            "classpath:jasperreport/SimpleReporter.jrxml", output,
            parameters, beanColDataSource,
        )

        (output.exists() && output.length() > 0) shouldBe true // JasperReportUtil 應產生非空 PDF 檔
    }

    test("exportPdfTest") {
        log.info("generating pdf file...")
        val outFile = File(tempDir, "ExampleReport.pdf")
        // 1. compile template ".jrxml" file
        val jasperReport = getJasperReport()

        // 2. parameters "empty"
        val parameters = getParameters()

        // 3. datasource "java object"
        val dataSource = getDataSource()

        val jasperPrint = JasperFillManager.fillReport(jasperReport, parameters, dataSource)
        JasperExportManager.exportReportToPdfFile(jasperPrint, outFile.absolutePath)

        (outFile.exists() && outFile.length() > 0) shouldBe true // JasperExportManager 應產生非空 PDF 檔
    }
})

private fun getJasperReport(): JasperReport {
    val template = ResourceUtils.getFile("classpath:jasperreport/ExampleReport.jrxml")
    return JasperCompileManager.compileReport(template.absolutePath)
}

private fun getParameters(): Map<String, Any> {
    val parameters = HashMap<String, Any>()
    parameters["createdBy"] = "Gary"
    return parameters
}

private fun getDataSource(): JRDataSource {
    val countries = LinkedList<Country>()
    countries.add(
        Country(
            "IS", "Iceland",
            "https://i.pinimg.com/originals/72/b4/49/72b44927f220151547493e528a332173.png",
        ),
    )
    // 9 other countries in GITHUB
    return JRBeanCollectionDataSource(countries)
}
