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

    /** 分頁 SQL 模板字串，於類別載入時從 classpath 資源檔讀取並快取，供所有子類別共用 */
    private static String pageingTemplate = null;

    private int page;                   // 當前頁碼
    private int pagesize;               // 每頁顯示筆數
    private int recordsTotal;           // 總資料筆數
    private int recordsFiltered;        // 總資料筆數(顯示於頁角)
    private List<String> orderBy;       // 需要使用哪些欄位排序

    static {
        // 透過 ResourceEnum 取得分頁 SQL 檔案的路徑資訊，並組合為完整相對路徑
        ResourceEnum resource = ResourceEnum.SQL.getResource("PAGING");
        StringBuilder path = new StringBuilder();
        path.append(resource.dir());
        path.append(resource.file());
        path.append(resource.extension());
        File file = FileUtil.getFileFromClasspath(path.toString());
        try {
            // 將分頁 SQL 模板一次讀入記憶體，避免每次查詢都進行 I/O
            pageingTemplate = FileUtil.readFileToString(file, StandardCharsets.UTF_8.name());
        } catch (IOException e) {
            e.getMessage();
        }

    }

    /**
     * 取得從資源檔載入的分頁 SQL 模板字串。
     * <p>
     * 子類別可透過此方法取得通用的分頁 SQL，再依需求組合實際的查詢語句。
     * </p>
     *
     * @return 分頁 SQL 模板字串；若初始化時讀取失敗則回傳 {@code null}
     */
    public String getPagingSQL() {
        return pageingTemplate;
    }

}
