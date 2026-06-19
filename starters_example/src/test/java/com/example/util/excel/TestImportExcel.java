package com.example.util.excel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.zipe.util.doc.ExcelLogs;
import com.zipe.util.doc.ExcelUtil;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestImportExcel {

  /** JUnit 提供的臨時目錄，測試結束自動清除，避免硬編路徑與環境相依。 */
  @TempDir
  Path tempDir;

  /**
   * 以 POI 建立含標題列與一筆資料列的 Excel 檔，依副檔名決定 xls（HSSF）或 xlsx（XSSF）。
   */
  private File createExcel(String fileName) throws Exception {
    File file = tempDir.resolve(fileName).toFile();
    try (Workbook workbook = fileName.endsWith(".xlsx") ? new XSSFWorkbook() : new HSSFWorkbook();
        FileOutputStream out = new FileOutputStream(file)) {
      Sheet sheet = workbook.createSheet("sheet1");
      Row header = sheet.createRow(0);
      header.createCell(0).setCellValue("name");
      header.createCell(1).setCellValue("age");
      Row data = sheet.createRow(1);
      data.createCell(0).setCellValue("張三");
      data.createCell(1).setCellValue("18");
      workbook.write(out);
    }
    return file;
  }

  @Test
  void importXls() throws Exception {
    File f = createExcel("test1.xls");

    ExcelLogs logs = new ExcelLogs();
    Collection<Map> importExcel =
        ExcelUtil.importExcel(f, Map.class, "yyyy/MM/dd HH:mm:ss", logs, 0);

    assertNotNull(importExcel);
    assertEquals(1, importExcel.size());
    for (Map m : importExcel) {
      System.out.println(m);
    }
  }

  @Test
  void importXlsx() throws Exception {
    File f = createExcel("test2.xlsx");

    ExcelLogs logs = new ExcelLogs();
    Collection<Map> importExcel =
        ExcelUtil.importExcel(f, Map.class, "yyyy/MM/dd HH:mm:ss", logs, 0);

    assertNotNull(importExcel);
    assertEquals(1, importExcel.size());
    for (Map m : importExcel) {
      System.out.println(m);
    }
  }
}
