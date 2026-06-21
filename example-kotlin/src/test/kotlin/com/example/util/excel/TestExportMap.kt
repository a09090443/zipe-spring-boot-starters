package com.example.util.excel

import com.zipe.util.doc.ExcelUtil
import io.kotest.core.spec.style.FunSpec
import java.io.File
import java.io.FileOutputStream
import java.util.Date

/**
 * 以 List&lt;Map&gt; 為資料來源的 Excel 匯出測試，標題列同樣以排序 Map 指定。
 */
class TestExportMap : FunSpec({

    test("exportXls") {
        val list = mutableListOf<Map<String, Any?>>()
        val map = linkedMapOf<String, Any?>(
            "name" to "",
            "age" to "",
            "birthday" to "",
            "sex" to "",
        )
        val map2 = linkedMapOf<String, Any?>(
            "name" to "測試是否是中文長度不能自動寬度.總統府地址:臺北市中正區重慶南路一段122號。",
            "age" to null,
            "sex" to null,
        )
        map["birthday"] = null
        val map3 = linkedMapOf<String, Any?>(
            "name" to "張三",
            "age" to 12,
            "sex" to "男",
            "birthday" to Date(),
        )
        list.add(map)
        list.add(map2)
        list.add(map3)
        val map1 = linkedMapOf(
            "name" to "姓名",
            "age" to "年齡",
            "birthday" to "出生日期",
            "sex" to "性别",
        )
        val f = File("D:/test.xls")
        FileOutputStream(f).use { out ->
            ExcelUtil.exportExcel(map1, list, out)
        }
    }
})
