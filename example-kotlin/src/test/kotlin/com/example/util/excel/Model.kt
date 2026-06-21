package com.example.util.excel

import com.zipe.util.doc.ExcelCell
import java.util.Date

/**
 * Excel 匯出測試用的資料模型。
 *
 * 各屬性以 [ExcelCell] 的 `index` 對應 Excel 欄位順序；
 * 由於 [ExcelCell] 的 `@Target` 為 FIELD，Kotlin 端以 `@field:ExcelCell` 套用至屬性的後備欄位。
 */
class Model(
    @field:ExcelCell(index = 0)
    var a: String?,
    @field:ExcelCell(index = 1)
    var b: String?,
    @field:ExcelCell(index = 2)
    var c: String?,
    @field:ExcelCell(index = 3)
    var d: Date?,
)
