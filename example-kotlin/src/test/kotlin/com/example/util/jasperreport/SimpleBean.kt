package com.example.util.jasperreport

/**
 * JasperReports 範例報表用的人物資料模型。
 *
 * 提供 name / occupation / place / country 四個可讀寫欄位，
 * 由 [SimpleBeanMaker] 建立並填入報表資料來源。
 */
class SimpleBean {
    var name: String? = null
    var occupation: String? = null
    var place: String? = null
    var country: String? = null
}
