package com.zipe.jdbc.criteria;

import com.zipe.enums.ResourceEnum;
import com.zipe.util.file.FileUtil;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 實現伺服器端分頁的資料物件<br>
 * P.S. 如果需要使用伺服器端分頁的SQL需要排序, 直接使用Paging.orderBy來排序。
 *
 * @author adam.yeh
 */
@Data
public abstract class Paging {

    private static String pageingTemplate = null;

    private int page;                   // 當前頁碼
    private int pagesize;               // 每頁顯示筆數
    private int recordsTotal;           // 總資料筆數
    private int recordsFiltered;        // 總資料筆數(顯示於頁角)
    private List<String> orderBy;       // 需要使用哪些欄位排序

    static {
        ResourceEnum resource = ResourceEnum.SQL.getResource("PAGING");
        StringBuilder path = new StringBuilder();
        path.append(resource.dir());
        path.append(resource.file());
        path.append(resource.extension());
        File file = FileUtil.getFileFromClasspath(path.toString());
        try {
            pageingTemplate = FileUtil.readFileToString(file, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            e.getMessage();
        }

    }

    public String getPagingSQL() {
        return pageingTemplate;
    }

}
