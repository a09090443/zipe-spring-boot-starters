package com.example.util.excel;

import com.zipe.util.doc.ExcelUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class TestExportMap {

  @Test
  void exportXls() throws IOException {
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("name", "");
    map.put("age", "");
    map.put("birthday", "");
    map.put("sex", "");
    Map<String, Object> map2 = new LinkedHashMap<String, Object>();
    map2.put("name", "測試是否是中文長度不能自動寬度.總統府地址:臺北市中正區重慶南路一段122號。");
    map2.put("age", null);
    map2.put("sex", null);
    map.put("birthday", null);
    Map<String, Object> map3 = new LinkedHashMap<String, Object>();
    map3.put("name", "張三");
    map3.put("age", 12);
    map3.put("sex", "男");
    map3.put("birthday", new Date());
    list.add(map);
    list.add(map2);
    list.add(map3);
    Map<String, String> map1 = new LinkedHashMap<>();
    map1.put("name", "姓名");
    map1.put("age", "年齡");
    map1.put("birthday", "出生日期");
    map1.put("sex", "性别");
    File f = new File("D:/test.xls");
    OutputStream out = new FileOutputStream(f);
    ExcelUtil.exportExcel(map1, list, out);
    out.close();
  }
}
