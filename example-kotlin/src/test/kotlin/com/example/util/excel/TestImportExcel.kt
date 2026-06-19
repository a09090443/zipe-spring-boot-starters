package com.example.util.excel

import com.zipe.util.doc.ExcelLogs
import com.zipe.util.doc.ExcelUtil
import io.kotest.core.spec.style.FunSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import org.apache.poi.hssf.usermodel.HSSFWorkbook
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream

/**
 * Excel 匯入測試：以 POI 動態建立 xls / xlsx 測試檔，再以 [ExcelUtil.importExcel] 讀回，
 * 驗證資料筆數。測試檔寫入 Kotest 提供的暫存目錄，避免硬編路徑與環境相依。
 */
class TestImportExcel : FunSpec({

    // Kotest 提供的暫存目錄，spec 結束後自動清除
    val tempDir = tempdir()

    /**
     * 以 POI 建立含標題列與一筆資料列的 Excel 檔，依副檔名決定 xls（HSSF）或 xlsx（XSSF）。
     */
    fun createExcel(fileName: String): File {
        val file = File(tempDir, fileName)
        val workbook: Workbook = if (fileName.endsWith(".xlsx")) XSSFWorkbook() else HSSFWorkbook()
        workbook.use { wb ->
            FileOutputStream(file).use { out ->
                val sheet = wb.createSheet("sheet1")
                val header = sheet.createRow(0)
                header.createCell(0).setCellValue("name")
                header.createCell(1).setCellValue("age")
                val data = sheet.createRow(1)
                data.createCell(0).setCellValue("張三")
                data.createCell(1).setCellValue("18")
                wb.write(out)
            }
        }
        return file
    }

    test("importXls") {
        val f = createExcel("test1.xls")

        val logs = ExcelLogs()
        val importExcel: Collection<Map<*, *>> =
            ExcelUtil.importExcel(f, Map::class.java, "yyyy/MM/dd HH:mm:ss", logs, 0)

        importExcel.shouldNotBeNull()
        importExcel shouldHaveSize 1
        importExcel.forEach { println(it) }
    }

    test("importXlsx") {
        val f = createExcel("test2.xlsx")

        val logs = ExcelLogs()
        val importExcel: Collection<Map<*, *>> =
            ExcelUtil.importExcel(f, Map::class.java, "yyyy/MM/dd HH:mm:ss", logs, 0)

        importExcel.shouldNotBeNull()
        importExcel shouldHaveSize 1
        importExcel.forEach { println(it) }
    }
})
