package com.example.util.jasperreport

/**
 * JasperReports 範例報表用的國家資料模型。
 *
 * 提供 code / name / url 三個欄位，供 [net.sf.jasperreports.engine.data.JRBeanCollectionDataSource]
 * 以 JavaBean 形式填入報表。
 */
class Country(
    var code: String?,
    var name: String?,
    var url: String?,
) {
    override fun toString(): String =
        "Country{code='$code', name='$name', url='$url'}"
}
