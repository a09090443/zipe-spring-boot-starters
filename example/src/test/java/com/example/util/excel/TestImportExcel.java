package com.example.util.excel;

import com.zipe.util.doc.ExcelLogs;
import com.zipe.util.doc.ExcelUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Map;

class TestImportExcel {

  @Test
  void importXls() throws FileNotFoundException {
    File f = new File("d:/tmp/test1.xls");

    ExcelLogs logs = new ExcelLogs();
    Collection<Map> importExcel = ExcelUtil.importExcel(f, Map.class, "yyyy/MM/dd HH:mm:ss", logs,
        0);

    for (Map m : importExcel) {
      System.out.println(m);
    }
  }

  @Test
  void importXlsx() throws FileNotFoundException {
    File f = new File("d:/tmp/test2.xlsx");
    InputStream inputStream = new FileInputStream(f);

    ExcelLogs logs = new ExcelLogs();
    Collection<Map> importExcel = ExcelUtil.importExcel(f, Map.class,
        "yyyy/MM/dd HH:mm:ss", logs, 0);

    for (Map m : importExcel) {
      System.out.println(m);
    }
  }
}
