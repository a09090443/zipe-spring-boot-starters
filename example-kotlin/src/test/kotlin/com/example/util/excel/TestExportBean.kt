package com.example.util.excel

import com.zipe.util.doc.ExcelUtil
import io.kotest.core.spec.style.FunSpec
import java.io.File
import java.io.FileOutputStream
import java.util.Date

/**
 * 以 Bean（[Model]）為資料來源的 Excel 匯出測試。
 *
 * 用排序的 Map 且 Map 的鍵應與 [com.zipe.util.doc.ExcelCell] 註解的 index 對應。
 */
class TestExportBean : FunSpec({

    test("exportXls") {
        // 用排序的Map且Map的鍵應與ExcelCell註解的index對應
        val map = linkedMapOf(
            "a" to "姓名",
            "b" to "年齡",
            "c" to "性別",
            "d" to "出生日期",
        )
        val dataset = mutableListOf<Any>()
        dataset.add(Model("", "", "", null))
        dataset.add(Model(null, null, null, null))
        dataset.add(Model("王五", "34", "男", Date()))
        val f = File("D:/test.xls")
        FileOutputStream(f).use { out ->
            ExcelUtil.exportExcel(map, dataset, out)
        }
    }
})
