package com.example.util.excel;

import com.zipe.util.doc.ExcelUtil;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

class TestExportBean {

    @Test
    void exportXls() throws IOException {
        //用排序的Map且Map的鍵應與ExcelCell註解的index對應
        Map<String, String> map = new LinkedHashMap<>();
        map.put("a", "姓名");
        map.put("b", "年齡");
        map.put("c", "性別");
        map.put("d", "出生日期");
        Collection<Object> dataset = new ArrayList<Object>();
        dataset.add(new Model("", "", "", null));
        dataset.add(new Model(null, null, null, null));
        dataset.add(new Model("王五", "34", "男", new Date()));
        File f = new File("D:/tmp/test.xls");
        OutputStream out = new FileOutputStream(f);

        ExcelUtil.exportExcel(map, dataset, out);
        out.close();
    }
}
