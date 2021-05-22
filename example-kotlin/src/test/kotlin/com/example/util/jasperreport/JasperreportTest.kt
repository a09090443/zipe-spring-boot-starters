package com.example.util.jasperreport

import net.sf.jasperreports.engine.JRDataSource
import net.sf.jasperreports.engine.JasperCompileManager
import net.sf.jasperreports.engine.JasperExportManager
import net.sf.jasperreports.engine.JasperFillManager
import net.sf.jasperreports.engine.JasperReport
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource
import org.junit.jupiter.api.Test
import org.springframework.util.ResourceUtils

class JasperreportTest {
    companion object {
        private const val OUT_FILE = "/home/zipe/tmp/KotlinReports.pdf"
    }

    @Test
    fun `export pdf file`() {

        val jasperReport = getJasperReport()

        val parameters = getParameters()

        val dataSource = getDataSource()

        val jasperPrint = JasperFillManager.fillReport(
            jasperReport,
            parameters,
            dataSource
        )
        JasperExportManager.exportReportToPdfFile(jasperPrint, OUT_FILE)
    }

    private fun getJasperReport(): JasperReport {
        val template = ResourceUtils.getFile("classpath:jasperreport/ExampleReport.jrxml")
        return JasperCompileManager.compileReport(template.absolutePath)
    }

    private fun getDataSource(): JRDataSource {
        val countries = listOf(
            Country(
                code = "IS",
                name = "Iceland",
                url = "https://i.pinimg.com/originals/72/b4/49/72b44927f220151547493e528a332173.png"
            )
        )
        return JRBeanCollectionDataSource(countries.toMutableList())
    }

    private fun getParameters() = mutableMapOf<String, Any>("createdBy" to "Gary")
}
