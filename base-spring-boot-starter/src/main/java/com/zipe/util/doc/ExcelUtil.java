package com.zipe.util.doc;

import com.zipe.util.string.StringConstant;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.beanutils.BeanComparator;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.commons.collections.comparators.ComparableComparator;
import org.apache.commons.collections.comparators.ComparatorChain;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRichTextString;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor.HSSFColorPredefined;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

/**
 * The <code>ExcelUtil</code> 與 {@link ExcelCell}搭配使用
 */
@Slf4j
public class ExcelUtil {

    /**
     * 用來驗證excel與Vo中的類型是否一致 <br>
     * Map<欄位類型,只能是哪些Cell類型>
     */
    private static Map<Class<?>, CellType[]> validateMap = new HashMap<>();

    static {
        validateMap.put(String[].class, new CellType[]{CellType.STRING});
        validateMap.put(Double[].class, new CellType[]{CellType.NUMERIC});
        validateMap.put(String.class, new CellType[]{CellType.STRING});
        validateMap.put(Double.class, new CellType[]{CellType.NUMERIC});
        validateMap.put(Date.class, new CellType[]{CellType.NUMERIC, CellType.STRING});
        validateMap.put(Integer.class, new CellType[]{CellType.NUMERIC});
        validateMap.put(Float.class, new CellType[]{CellType.NUMERIC});
        validateMap.put(Long.class, new CellType[]{CellType.NUMERIC});
        validateMap.put(Boolean.class, new CellType[]{CellType.BOOLEAN});
    }

    /**
     * 獲取cell類型的文字描述
     *
     * @param cellType <pre>
     *                                                                 CellType.BLANK
     *                                                                 CellType.BOOLEAN
     *                                                                 CellType.ERROR
     *                                                                 CellType.FORMULA
     *                                                                 CellType.NUMERIC
     *                                                                 CellType.STRING
     *                                                                 </pre>
     * @return
     */
    private static String getCellTypeByInt(CellType cellType) {
        if (cellType == CellType.BLANK) {
            return "Null type";
        } else if (cellType == CellType.BOOLEAN) {
            return "Boolean type";
        } else if (cellType == CellType.ERROR) {
            return "Error type";
        } else if (cellType == CellType.FORMULA) {
            return "Formula type";
        } else if (cellType == CellType.NUMERIC) {
            return "Numeric type";
        } else if (cellType == CellType.STRING) {
            return "String type";
        } else {
            return "Unknown type";
        }
    }

    /**
     * 獲取單元格值
     *
     * @param cell
     * @return
     */
    private static Object getCellValue(Cell cell) {
        if (cell == null
                || (cell.getCellType() == CellType.STRING && isBlank(cell
                .getStringCellValue()))) {
            return null;
        }
        CellType cellType = cell.getCellType();
        if (cellType == CellType.BLANK) {
            return null;
        } else if (cellType == CellType.BOOLEAN) {
            return cell.getBooleanCellValue();
        } else if (cellType == CellType.ERROR) {
            return cell.getErrorCellValue();
        } else if (cellType == CellType.FORMULA) {
            try {
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue();
                } else {
                    return cell.getNumericCellValue();
                }
            } catch (IllegalStateException e) {
                return cell.getRichStringCellValue();
            }
        } else if (cellType == CellType.NUMERIC) {
            if (DateUtil.isCellDateFormatted(cell)) {
                return cell.getDateCellValue();
            } else {
                return cell.getNumericCellValue();
            }
        } else if (cellType == CellType.STRING) {
            return cell.getStringCellValue();
        } else {
            return null;
        }
    }

    /**
     * 設定excel 欄位的Style
     *
     * @param style
     * @param font
     * @param param
     * @param num
     */
    private static void getCellStyle(XSSFCellStyle style, Font font, String param, Integer... num) {
//        style.setFillBackgroundColor(new XSSFColor(new java.awt.Color(128, 0, 128)));
//        palette.setColorAtIndex(HSSFColorPredefined.GREEN.getIndex(), (byte) 34, (byte) 139, (byte) 34);//綠
//        palette.setColorAtIndex(HSSFColorPredefined.GREY_25_PERCENT.getIndex(), (byte) 230, (byte) 230, (byte) 250);//淺灰
//        palette.setColorAtIndex(HSSFColorPredefined.GREY_40_PERCENT.getIndex(), (byte) 220, (byte) 220, (byte) 220);//深灰
        if (param.equals(StringConstant.PARAM_TITLE) || param.equals(StringConstant.PARAM_TITLEM)) {
            font.setBold(true);
            font.setColor(HSSFColorPredefined.GREEN.getIndex());
            style.setFont(font);
            style.setWrapText(true);
            if (param.equals(StringConstant.PARAM_TITLEM)) {
                style.setAlignment(HorizontalAlignment.CENTER); // 水平置中
            }
            style.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直置中
        } else if (param.equals(StringConstant.PARAM_CONTENT) || param.equals(StringConstant.PARAM_FOOTER)
                || param.equals(StringConstant.PARAM_CONTENTM) || param.equals(StringConstant.PARAM_FOOTERM)) {
            style.setFillForegroundColor(HSSFColorPredefined.GREY_40_PERCENT.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            style.setWrapText(true);
            if (param.equals(StringConstant.PARAM_CONTENTM) || param.equals(StringConstant.PARAM_FOOTERM)) {
                style.setAlignment(HorizontalAlignment.CENTER); // 水平置中
            }
            style.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直置中
        } else if (param.equals(StringConstant.PARAM_HEADER) || param.equals(StringConstant.PARAM_HEADERM)) {
            style.setFillForegroundColor(HSSFColorPredefined.GREEN.getIndex());
            style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            font.setColor(HSSFColorPredefined.WHITE.getIndex());
            style.setFont(font);
            style.setWrapText(true);
            if (param.equals(StringConstant.PARAM_HEADERM)) {
                style.setAlignment(HorizontalAlignment.CENTER); // 水平置中
            }
            style.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直置中
        } else if (param.equals(StringConstant.PARAM_BODY) || param.equals(StringConstant.PARAM_BODYM)) {
            if (num != null) {
                if (num[0] == 1) {
                    style.setFillForegroundColor(HSSFColorPredefined.GREY_25_PERCENT.getIndex());
                    style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
                }
            }
            style.setWrapText(true);
            if (param.equals(StringConstant.PARAM_BODYM)) {
                style.setAlignment(HorizontalAlignment.CENTER); // 水平置中
            }
            style.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直置中
        } else if (param.equals(StringConstant.PARAM_TOTAL) || param.equals(StringConstant.PARAM_TOTALM)) {
            if (num != null) {
                if (num[0] == 0) {
                    font.setBold(true);
                    font.setColor(HSSFColorPredefined.GREEN.getIndex());
                    style.setFont(font);
                }
            }
            style.setWrapText(true);
            if (param.equals(StringConstant.PARAM_TOTALM)) {
                style.setAlignment(HorizontalAlignment.CENTER); // 水平置中
            }
            style.setVerticalAlignment(VerticalAlignment.CENTER); // 垂直置中
        }
    }

    /**
     * 判斷字串是否為數字
     *
     * @param str
     * @return boolean
     */
    private static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    /**
     * headers 併入data set的資料行內
     * isAddSheet 判斷是否新增 sheet 到已存在的 excel檔
     *
     * @param dataset
     * @param file
     * @param isAddSheet
     */
    public static <T> void exportExcel(String sheetName, Collection<T> dataset, File file, boolean isAddSheet) {
        XSSFWorkbook workbook = null;
        if (isAddSheet && file.exists()) {
            try {
                FileInputStream fileInput = new FileInputStream(file);
                workbook = new XSSFWorkbook(fileInput);
            } catch (IOException e) {
                log.error(e.toString(), e);
            }
        } else {
            // 宣告一個工作薄
            workbook = new XSSFWorkbook();
        }

        XSSFSheet sheet = workbook.createSheet(sheetName);
        if (dataset == null) {
            return;
        }
        String pattern = "yyyy/MM/dd";//日期格式

        XSSFRow row = sheet.createRow(0);
        Iterator<T> it = dataset.iterator();
        int index = 0;
        int mergeRows = -1;
        int mergeColumns = -1;
        int mergeCount = 0;
        int changeColarRows = 0;
        int changeColarColumns = 0;
        int lastTime = 0;
        int follow = 0;
        Map<Integer, Map<Integer, Integer>> merge = new HashMap<Integer, Map<Integer, Integer>>();
        while (it.hasNext()) {
            row = sheet.createRow(index);
            T t = it.next();
            String type = "";
            try {
                if (t instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) t;
                    int cellNum = 0;
                    //遍歷列名
                    Iterator<String> it2 = map.keySet().iterator();
                    while (it2.hasNext()) {
                        String key = it2.next();
                        Object value = map.get(key);
                        XSSFCell cell = row.createCell(cellNum);
                        cellNum = setCellValue(cell, value, pattern, cellNum, null, row);
                        cellNum++;
                    }
                } else {
                    List<FieldForSortting> fields = sortFieldByAnno(t.getClass());
                    int cellNum = 0;
                    for (int i = 0; i < fields.size(); i++) {
                        log.info(i + "." + "h_cellNum = " + cellNum);
                        XSSFCell cell = row.createCell(cellNum);
                        Field field = fields.get(i).getField();
                        field.setAccessible(true);
                        Object value = field.get(t);
                        if (value == null) {
                            log.info("run null");
                            log.info("mergeColumns = " + mergeColumns + ",mergeRows = " + mergeRows);
                            if (mergeColumns > 0 && cellNum > 0) {
                                Map<Integer, Integer> temp = new HashMap<Integer, Integer>();
                                int per = 0;
                                temp.put(per++, index);
                                temp.put(per++, mergeRows == -1 ? index : (index + mergeRows));
                                temp.put(per++, cellNum - mergeColumns - 1);
                                temp.put(per++, cellNum - 1);
                                if (follow == 0) {
                                    merge.put(mergeCount, temp);
                                    log.info("run,4,put[" + mergeCount + "]=(" + index + "," + (mergeRows == -1 ? index : (index + mergeRows)) + "," + (cellNum - mergeColumns - 1) + "," + (cellNum - 1) + ")");

                                } else {
                                    merge.put(mergeCount - 1, temp);
                                }
                                mergeCount++;
                                mergeRows = -1;
                                mergeColumns = -1;
                            } else if (mergeRows > 0 && cellNum > 0) {
                                Map<Integer, Integer> temp = new HashMap<Integer, Integer>();
                                int per = 0;
                                temp.put(per++, index);
                                temp.put(per++, index + mergeRows);
                                temp.put(per++, cellNum - 1);
                                temp.put(per++, cellNum - 1);
                                if (follow == 0) {
                                    merge.put(mergeCount - 1, temp);
                                } else {
                                    merge.put(mergeCount, temp);
                                }
                                log.info("run,1,put[" + (mergeCount - 1) + "]=(" + index + ","
                                        + (index + mergeRows) + "," + (cellNum) + "," + (cellNum)
                                        + ")");
                                mergeCount++;
                                mergeRows = -1;
                            }
                            continue;
                            //break;
                        }
                        if (value instanceof String) {
                            if (String.valueOf(value).contains(StringConstant.EQUALS)) {

                                log.info("run true");
                                if (String.valueOf(value).equals(StringConstant.PARAM_SKIP_A_LINE)) {
                                    follow = 0;
                                    break;
                                }
                                if (String.valueOf(value).equals(StringConstant.PARAM_BLANK)) {
                                    cellNum++;
                                    follow = 0;
                                    changeColarRows = 0;
                                    continue;
                                }

                                String[] datas =
                                        StringUtils.splitByWholeSeparatorPreserveAllTokens(
                                                String.valueOf(value), StringConstant.EQUALS);

                                if (datas[0].equals(StringConstant.UNDERLINE_PARAM)) {
                                    String kind = datas[0] + "=" + datas[1];
                                    if (kind.equals(StringConstant.PARAM_MERGE_COLUMNS) && datas.length > 2 && cellNum > 0) {
                                        if (isInteger(datas[2])) {
                                            if (mergeColumns == -1) {
                                                mergeColumns = Integer.valueOf(datas[2]);
                                                cellNum += mergeColumns;
                                            }
                                            follow = 0;
                                            continue;
                                        }
                                    }
                                    if (datas.length > 2 && cellNum == 0) {
                                        if (isInteger(datas[2])) {
                                            mergeRows = Integer.valueOf(datas[2]);
                                            lastTime = mergeRows;
                                        }
                                    }
                                    if (cellNum == 0) {
                                        type = kind;
                                    }
                                    follow = 0;
                                    continue;
                                }
                                follow = 0;
                            } else {
                                follow = 1;
                                log.info("run false");
                                log.info("mergeColumns = " + mergeColumns + ",mergeRows = " + mergeRows);
                                if (mergeColumns > 0 && cellNum > 0) {
                                    Map<Integer, Integer> temp = new HashMap<Integer, Integer>();
                                    int per = 0;
                                    temp.put(per++, index);
                                    temp.put(per++, mergeRows == -1 ? index : (index + mergeRows));
                                    temp.put(per++, cellNum - mergeColumns - 1);
                                    temp.put(per++, cellNum - 1);
                                    log.info("*cellNum=" + cellNum);
                                    log.info("*mergeColumns=" + mergeColumns);
                                    if (follow == 1) {
                                        merge.put(mergeCount, temp);
                                        log.info("run,2,put[" + mergeCount + "]=(" + index + "," + (mergeRows == -1 ? index : (index + mergeRows)) + "," + (cellNum - mergeColumns - 1) + "," + (cellNum - 1) + ")");
                                        mergeCount++;
                                    } else {
                                        merge.put(mergeCount - 1, temp);
                                        log.info("run,2,put[" + (mergeCount - 1) + "]=(" + index + "," + (mergeRows == -1 ? index : (index + mergeRows)) + "," + (cellNum - mergeColumns - 1) + "," + (cellNum - 1) + ")");
                                    }
                                    mergeColumns = -1;
                                    follow = 1;
                                } else if (mergeRows > 0 && cellNum > 0) {
                                    Map<Integer, Integer> temp = new HashMap<Integer, Integer>();
                                    int per = 0;
                                    temp.put(per++, index);
                                    temp.put(per++, index + mergeRows);
                                    temp.put(per++, cellNum - 1);
                                    temp.put(per++, cellNum - 1);
                                    merge.put(mergeCount, temp);
                                    log.info("cellnumber=" + cellNum);
                                    log.info("run,3,put[" + mergeCount + "]=(" + index + "," + (index + mergeRows) + "," + (mergeColumns <= 0 ? cellNum : (cellNum)) + "," + (mergeColumns <= 0 ? cellNum : (cellNum)) + ")");
                                    mergeCount++;
                                    follow = 1;
                                }
                            }
                        }
                        if (!type.equals("")) {
                            XSSFCellStyle style = workbook.createCellStyle();
                            //  XSSFPalette palette = workbook.getCustomPalette();
                            Font font = workbook.createFont();
                            if (type.equals(StringConstant.PARAM_HEADER) || type.equals(StringConstant.PARAM_HEADERM)) {
                                changeColarRows = 0;
                            }
                            if (type.equals(StringConstant.PARAM_BODY) || type.equals(StringConstant.PARAM_BODYM)) {
                                getCellStyle(style, font, type, changeColarRows);

                            } else if (type.equals(StringConstant.PARAM_TOTAL) || type.equals(StringConstant.PARAM_TOTALM)) {
                                getCellStyle(style, font, type, changeColarColumns);
                                if (changeColarColumns == 0) {
                                    changeColarColumns = 1;
                                } else {
                                    changeColarColumns = 0;
                                }
                            } else {
                                getCellStyle(style, font, type);
                            }
                            cell.setCellStyle(style);
                        }
                        cellNum = setCellValue(cell, value, pattern, cellNum, field, row);
                        log.info(i + "." + "f_cellNum = " + cellNum);
                        cellNum++;

                    }
                    if (type.equals(StringConstant.PARAM_BODY) || type.equals(StringConstant.PARAM_BODYM)) {
                        if (changeColarRows == 0) {
                            changeColarRows = 1;
                        } else {
                            changeColarRows = 0;
                        }
                    }
                    type = "";
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
            if (lastTime > 0) {
                index = index + lastTime + 1;
            } else {
                index++;
            }
            lastTime = 0;
            mergeCount++;
        }

        if (merge.size() > 0) {
            log.info("執行結果:");
            for (Entry<Integer, Map<Integer, Integer>> entry : merge.entrySet()) {
                log.info("merge [" + entry.getKey() + "]=" + entry.getValue().get(0) + "," + entry.getValue().get(1) + "," + entry.getValue().get(2) + "," + entry.getValue().get(3));
                sheet.addMergedRegion(new CellRangeAddress(
                        entry.getValue().get(0), entry.getValue().get(1), entry.getValue().get(2), entry.getValue().get(3)));
            }
        }
        FileOutputStream fileOutput;
        try {
            fileOutput = new FileOutputStream(file);
            workbook.write(fileOutput);
            fileOutput.close();
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
    }

    /**
     * headers 的 key和 data set 的key 相等時為同一組;
     * headers 有值,而data set 無值表示只有顯示標題
     * headers 必需有值可單獨存在
     * data set可無值,不可單獨存在,須與headers搭配
     *
     * @param headers
     * @param dataset
     * @param out
     */
    @SuppressWarnings("resource")
    public static <T> void exportExcel(Map<Integer, Map<Integer, String>> headers,
                                       Map<Integer, Collection<T>> dataset, OutputStream out) {
        // 宣告一個工作薄
        HSSFWorkbook workbook = new HSSFWorkbook();
        HSSFSheet sheet = workbook.createSheet();
        if (headers == null) {
            return;
        }
        String pattern = "yyyy-MM-dd";//日期格式
        int index = 0;

        for (Entry<Integer, Map<Integer, String>> entry : headers.entrySet()) {
            HSSFRow row = sheet.createRow(index);
            Set<Integer> keys = entry.getValue().keySet();
            int key = 0;
            int rowNum = 0;
            for (Entry<Integer, String> e : entry.getValue().entrySet()) {
                row.createCell(rowNum++).setCellValue(e.getValue());
            }
            rowNum = 0;
            if (dataset != null) {
                if (dataset.containsKey(entry.getKey())) {

                    Iterator<T> it = dataset.get(entry.getKey()).iterator();
                    while (it.hasNext()) {
                        index++;
                        row = sheet.createRow(index);
                        T t = it.next();
                        try {
                            if (t instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<Integer, Object> map = (Map<Integer, Object>) t;
                                int cellNum = 0;
                                //遍歷列名
                                Iterator<Integer> it2 = keys.iterator();
                                while (it2.hasNext()) {
                                    key = it2.next();
                                    if (!headers.containsKey(key)) {
                                        log.error("Map 中 不存在 key [" + key + "]");
                                        continue;
                                    }
                                    Object value = map.get(key);
                                    HSSFCell cell = row.createCell(cellNum);

                                    cellNum = setCellValue(cell, value, pattern, cellNum, null, row);

                                    cellNum++;
                                }
                            } else {
                                List<FieldForSortting> fields = sortFieldByAnno(t.getClass());
                                int cellNum = 0;
                                for (int i = 0; i < fields.size(); i++) {
                                    HSSFCell cell = row.createCell(cellNum);
                                    Field field = fields.get(i).getField();
                                    field.setAccessible(true);
                                    Object value = field.get(t);

                                    cellNum = setCellValue(cell, value, pattern, cellNum, field, row);

                                    cellNum++;
                                }
                            }
                        } catch (Exception e) {
                            log.error(e.toString(), e);
                        }
                    }
                }
            }
            index++;
        }
        try {
            workbook.write(out);
            if (out != null) {
                out.close();
            }
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
    }

    /**
     * 利用JAVA的反射機制，將放置在JAVA集合中並且符號一定條件的數據以EXCEL 的形式輸出到指定IO設備上<br>
     * 用於單個sheet
     *
     * @param <T>
     * @param headers 表格屬性列名數組
     * @param dataset 需要顯示的數據集合,集合中一定要放置符合javabean風格的類的對象。此方法支持的
     *                javabean屬性的數據類型有基本數據類型及String,Date,String[],Double[]
     * @param out     與輸出設備關聯的流對象，可以將EXCEL文檔導出到本地文件或者網絡中
     */
    public static <T> void exportExcel(Map<String, String> headers, Collection<T> dataset, OutputStream out) {
        exportExcel(headers, dataset, out, null);
    }

    /**
     * 利用JAVA的反射機制，將放置在JAVA集合中並且符號一定條件的數據以EXCEL 的形式輸出到指定IO設備上<br>
     * 用於單個sheet
     *
     * @param <T>
     * @param headers 表格屬性列名數組
     * @param dataset 需要顯示的數據集合,集合中一定要放置符合javabean風格的類的對象。此方法支持的
     *                javabean屬性的數據類型有基本數據類型及String,Date,String[],Double[]
     * @param out     與輸出設備關聯的流對象，可以將EXCEL文檔導出到本地文件或者網絡中
     * @param pattern 如果有時間數據，設定輸出格式。默認為"yyy-MM-dd"
     */
    public static <T> void exportExcel(Map<String, String> headers, Collection<T> dataset, OutputStream out,
                                       String pattern) {
        // 声明一个工作薄
        @SuppressWarnings("resource")
        HSSFWorkbook workbook = new HSSFWorkbook();
        // 生成一个表格
        HSSFSheet sheet = workbook.createSheet();

        write2Sheet(sheet, headers, dataset, pattern);
        try {
            workbook.write(out);
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
    }

    public static void exportExcel(String[][] datalist, OutputStream out, boolean autoColumnWidth) {
        try {
            // 聲明一個工作薄
            @SuppressWarnings("resource")
            HSSFWorkbook workbook = new HSSFWorkbook();
            // 生成一個表格
            HSSFSheet sheet = workbook.createSheet();

            for (int i = 0; i < datalist.length; i++) {
                String[] r = datalist[i];
                HSSFRow row = sheet.createRow(i);
                for (int j = 0; j < r.length; j++) {
                    HSSFCell cell = row.createCell(j);
                    //cell max length 32767
                    if (r[j] != null && r[j].length() > 32767) {
                        r[j] = "--此字段过长(超过32767),已被截断--" + r[j];
                        r[j] = r[j].substring(0, 32766);
                    }
                    cell.setCellValue(r[j]);
                }
            }
            //自動列寬
            if (autoColumnWidth) {
                if (datalist.length > 0) {
                    int colcount = datalist[0].length;
                    for (int i = 0; i < colcount; i++) {
                        sheet.autoSizeColumn(i);
                    }
                }
            }
            workbook.write(out);
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
    }

    public static void exportExcel(String[][] datalist, OutputStream out) {
        exportExcel(datalist, out, true);
    }

    /**
     * 利用JAVA的反射機制，將放置在JAVA集合中並且符號一定條件的數據以EXCEL 的形式輸出到指定IO設備上<br>
     * 用於多個sheet
     *
     * @param <T>
     * @param sheets {@link ExcelSheet}的集合
     * @param out    與輸出設備關聯的流對象，可以將EXCEL文檔導出到本地文件或者網絡中
     */
    public static <T> void exportExcel(List<ExcelSheet<T>> sheets, OutputStream out) {
        exportExcel(sheets, out, null);
    }

    /**
     * 利用JAVA的反射機制，將放置在JAVA集合中並且符號一定條件的數據以EXCEL 的形式輸出到指定IO設備上<br>
     * 用於多個sheet
     *
     * @param <T>
     * @param sheets  {@link ExcelSheet}的集合
     * @param out     與輸出設備關聯的流對象，可以將EXCEL文檔導出到本地文件或者網絡中
     * @param pattern 如果有時間數據，設定輸出格式。默認為"yyy-MM-dd"
     */
    public static <T> void exportExcel(List<ExcelSheet<T>> sheets, OutputStream out, String pattern) {
        if (CollectionUtils.isEmpty(sheets)) {
            return;
        }
        // 聲明一個工作薄
        @SuppressWarnings("resource")
        HSSFWorkbook workbook = new HSSFWorkbook();
        for (ExcelSheet<T> sheet : sheets) {
            // 生成一個表格
            HSSFSheet hssfSheet = workbook.createSheet(sheet.getSheetName());
            write2Sheet(hssfSheet, sheet.getHeaders(), sheet.getDataset(), pattern);
        }
        try {
            workbook.write(out);
        } catch (IOException e) {
            log.error(e.toString(), e);
        }
    }

    /**
     * 每個sheet的寫入
     *
     * @param sheet   頁簽
     * @param headers 表頭
     * @param dataset 數據集合
     * @param pattern 日期格式
     */
    private static <T> void write2Sheet(HSSFSheet sheet, Map<String, String> headers, Collection<T> dataset,
                                        String pattern) {
        //時間格式默認"yyyy-MM-dd"
        if (isBlank(pattern)) {
            pattern = "yyyy-MM-dd";
        }
        // 產生表格標題行
        HSSFRow row = sheet.createRow(0);
        // 標題行轉中文
        Set<String> keys = headers.keySet();
        Iterator<String> it1 = keys.iterator();
        String key = "";    //存放臨時鍵變量
        int c = 0;   //標題列數
        while (it1.hasNext()) {
            key = it1.next();
            if (headers.containsKey(key)) {
                HSSFCell cell = row.createCell(c);
                HSSFRichTextString text = new HSSFRichTextString(headers.get(key));
                cell.setCellValue(text);
                c++;
            }
        }

        // 遍歷集合數據，產生數據行
        Iterator<T> it = dataset.iterator();
        int index = 0;
        while (it.hasNext()) {
            index++;
            row = sheet.createRow(index);
            T t = it.next();
            try {
                if (t instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = (Map<String, Object>) t;
                    int cellNum = 0;
                    //遍歷列名
                    Iterator<String> it2 = keys.iterator();
                    while (it2.hasNext()) {
                        key = it2.next();
                        if (!headers.containsKey(key)) {
                            log.error("Map 中 不存在 key [" + key + "]");
                            continue;
                        }
                        Object value = map.get(key);
                        HSSFCell cell = row.createCell(cellNum);

                        cellNum = setCellValue(cell, value, pattern, cellNum, null, row);

                        cellNum++;
                    }
                } else {
                    List<FieldForSortting> fields = sortFieldByAnno(t.getClass());
                    int cellNum = 0;
                    for (int i = 0; i < fields.size(); i++) {
                        HSSFCell cell = row.createCell(cellNum);
                        Field field = fields.get(i).getField();
                        field.setAccessible(true);
                        Object value = field.get(t);

                        cellNum = setCellValue(cell, value, pattern, cellNum, field, row);

                        cellNum++;
                    }
                }
            } catch (Exception e) {
                log.error(e.toString(), e);
            }
        }
        // 設定自動寬度
        for (int i = 0; i < headers.size(); i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private static int setCellValue(HSSFCell cell, Object value, String pattern, int cellNum, Field field, HSSFRow row) {
        String textValue = null;
        if (value instanceof Integer) {
            int intValue = (Integer) value;
            cell.setCellValue(intValue);
        } else if (value instanceof Float) {
            float fValue = (Float) value;
            cell.setCellValue(fValue);
        } else if (value instanceof Double) {
            double dValue = (Double) value;
            cell.setCellValue(dValue);
        } else if (value instanceof Long) {
            long longValue = (Long) value;
            cell.setCellValue(longValue);
        } else if (value instanceof Boolean) {
            boolean bValue = (Boolean) value;
            cell.setCellValue(bValue);
        } else if (value instanceof Date) {
            Date date = (Date) value;
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            textValue = sdf.format(date);
        } else if (value instanceof String[]) {
            String[] strArr = (String[]) value;
            for (int j = 0; j < strArr.length; j++) {
                String str = strArr[j];
                cell.setCellValue(str);
                if (j != strArr.length - 1) {
                    cellNum++;
                    cell = row.createCell(cellNum);
                }
            }
        } else if (value instanceof Double[]) {
            Double[] douArr = (Double[]) value;
            for (int j = 0; j < douArr.length; j++) {
                Double val = douArr[j];
                // 值不為空則set Value
                if (val != null) {
                    cell.setCellValue(val);
                }

                if (j != douArr.length - 1) {
                    cellNum++;
                    cell = row.createCell(cellNum);
                }
            }
        } else {
            // 其它數據類型都當作字符串簡單處理
            String empty = "";
            if (field != null) {
                ExcelCell anno = field.getAnnotation(ExcelCell.class);
                if (anno != null) {
                    empty = anno.defaultValue();
                }
            }
            textValue = value == null ? empty : value.toString();
        }
        if (textValue != null) {
            HSSFRichTextString richString = new HSSFRichTextString(textValue);
            cell.setCellValue(richString);
        }
        return cellNum;
    }

    private static int setCellValue(XSSFCell cell, Object value, String pattern, int cellNum, Field field, XSSFRow row) {
        String textValue = null;
        if (value instanceof Integer) {
            int intValue = (Integer) value;
            cell.setCellValue(intValue);
        } else if (value instanceof Float) {
            float fValue = (Float) value;
            cell.setCellValue(fValue);
        } else if (value instanceof Double) {
            double dValue = (Double) value;
            cell.setCellValue(dValue);
        } else if (value instanceof Long) {
            long longValue = (Long) value;
            cell.setCellValue(longValue);
        } else if (value instanceof Boolean) {
            boolean bValue = (Boolean) value;
            cell.setCellValue(bValue);
        } else if (value instanceof Date) {
            Date date = (Date) value;
            SimpleDateFormat sdf = new SimpleDateFormat(pattern);
            textValue = sdf.format(date);
        } else if (value instanceof String[]) {
            String[] strArr = (String[]) value;
            for (int j = 0; j < strArr.length; j++) {
                String str = strArr[j];
                cell.setCellValue(str);
                if (j != strArr.length - 1) {
                    cellNum++;
                    cell = row.createCell(cellNum);
                }
            }
        } else if (value instanceof Double[]) {
            Double[] douArr = (Double[]) value;
            for (int j = 0; j < douArr.length; j++) {
                Double val = douArr[j];
                // 值不為空則set Value
                if (val != null) {
                    cell.setCellValue(val);
                }

                if (j != douArr.length - 1) {
                    cellNum++;
                    cell = row.createCell(cellNum);
                }
            }
        } else {
            // 其它數據類型都當作字符串簡單處理
            String empty = "";
            if (field != null) {
                ExcelCell anno = field.getAnnotation(ExcelCell.class);
                if (anno != null) {
                    empty = anno.defaultValue();
                }
            }
            textValue = value == null ? empty : value.toString();
        }
        if (textValue != null) {
            XSSFRichTextString richString = new XSSFRichTextString(textValue);
            cell.setCellValue(richString);
        }
        return cellNum;
    }

    /**
     * 把Excel的數據封裝成voList
     *
     * @param excelFile  讀取的檔案
     * @param clazz      vo的Class
     * @param pattern    如果有時間數據，設定輸入格式。默認為"yyy-MM-dd"
     * @param logs       錯誤log集合
     * @param arrayCount 如果vo中有數組類型,那就按照index順序,把數組應該有幾個值寫上.
     * @return voList
     * @throws RuntimeException
     */
    @SuppressWarnings({"unchecked", "resource"})
    public static <T> Collection<T> importExcel(File excelFile, Class<T> clazz, String pattern, ExcelLogs logs, Integer... arrayCount) {
        Workbook workBook = null;
        POIFSFileSystem fs;
        try {
            if (excelFile.isFile() && excelFile.exists()) {
                String[] split = excelFile.getName().split("\\.");
                //根據檔案字尾（xls/xlsx）進行判斷
                if ("xls".equals(split[1])) {
                    FileInputStream inputStream = new FileInputStream(excelFile);
                    fs = new POIFSFileSystem(inputStream);
                    workBook = new HSSFWorkbook(fs);
                } else if ("xlsx".equals(split[1])) {
                    FileInputStream inputStream = new FileInputStream(excelFile);
                    workBook = new XSSFWorkbook(inputStream);
                } else {
                    log.error("load excel file error", "File type error!!");
                    throw new Exception("Excel file type error!!");
                }
            } else {
                throw new FileNotFoundException(excelFile.getAbsolutePath() + " not found.");
            }
        } catch (Exception e) {
            log.error("load excel file error", e);
            return null;
        }
        List<T> list = new ArrayList<>();
        Sheet sheet = workBook.getSheetAt(0);
        Iterator<Row> rowIterator = sheet.rowIterator();
        try {
            List<ExcelLog> logList = new ArrayList<>();
            // Map<title,index>
            Map<String, Integer> titleMap = new HashMap<>();

            while (rowIterator.hasNext()) {
                Row row = rowIterator.next();
                if (row.getRowNum() == 0) {
                    if (clazz == Map.class) {
                        // 解析map用的key,就是excel標題行
                        Iterator<Cell> cellIterator = row.cellIterator();
                        Integer index = 0;
                        while (cellIterator.hasNext()) {
                            String value = cellIterator.next().getStringCellValue();
                            titleMap.put(value, index);
                            index++;
                        }
                    }
                    continue;
                }
                // 整行都空，就跳過
                boolean allRowIsNull = true;
                Iterator<Cell> cellIterator = row.cellIterator();
                while (cellIterator.hasNext()) {
                    Object cellValue = getCellValue(cellIterator.next());
                    if (cellValue != null) {
                        allRowIsNull = false;
                        break;
                    }
                }
                if (allRowIsNull) {
                    log.warn("Excel row " + row.getRowNum() + " all row value is null!");
                    continue;
                }
                StringBuilder log = new StringBuilder();
                if (clazz == Map.class) {
                    Map<String, Object> map = new HashMap<>();
                    for (String k : titleMap.keySet()) {
                        Integer index = titleMap.get(k);
                        Cell cell = row.getCell(index);
                        // 判空
                        if (cell == null) {
                            map.put(k, null);
                        } else {
                            String value = Objects.isNull(getCellValue(cell)) ? null : getCellValue(cell).toString();
                            map.put(k, value);
                        }
                    }
                    list.add((T) map);

                } else {
                    T t = clazz.newInstance();
                    int arrayIndex = 0;// 標識當前第幾個數組了
                    int cellIndex = 0;// 標識當前讀到這一行的第幾個cell了
                    List<FieldForSortting> fields = sortFieldByAnno(clazz);
                    for (FieldForSortting ffs : fields) {
                        Field field = ffs.getField();
                        field.setAccessible(true);
                        if (field.getType().isArray()) {
                            Integer count = arrayCount[arrayIndex];
                            Object[] value;
                            if (field.getType().equals(String[].class)) {
                                value = new String[count];
                            } else {
                                // 目前只支持String[]和Double[]
                                value = new Double[count];
                            }
                            for (int i = 0; i < count; i++) {
                                Cell cell = row.getCell(cellIndex);
                                String errMsg = validateCell(cell, field, cellIndex);
                                if (isBlank(errMsg)) {
                                    value[i] = getCellValue(cell);
                                } else {
                                    log.append(errMsg);
                                    log.append(";");
                                    logs.setHasError(true);
                                }
                                cellIndex++;
                            }
                            field.set(t, value);
                            arrayIndex++;
                        } else {
                            Cell cell = row.getCell(cellIndex);
                            String errMsg = validateCell(cell, field, cellIndex);
                            if (isBlank(errMsg)) {
                                Object value = null;
                                // 處理特殊情況,Excel中的String,轉換成Bean的Date
                                if (field.getType().equals(Date.class)
                                        && cell.getCellType() == CellType.STRING) {
                                    Object strDate = getCellValue(cell);
                                    try {
                                        if (null != strDate) {
                                            value = new SimpleDateFormat(pattern).parse(strDate.toString());
                                        }
                                    } catch (ParseException e) {

                                        errMsg =
                                                MessageFormat.format("the cell [{0}] can not be converted to a date ",
                                                        CellReference.convertNumToColString(cell.getColumnIndex()));
                                    }
                                } else {
                                    value = getCellValue(cell);
                                    // 處理特殊情況,excel的value為String,且bean中為其他,且defaultValue不為空,那就=defaultValue
                                    ExcelCell annoCell = field.getAnnotation(ExcelCell.class);
                                    if (value instanceof String && !field.getType().equals(String.class)
                                            && isNotBlank(annoCell.defaultValue())) {
                                        value = annoCell.defaultValue();
                                    }
                                }
                                field.set(t, value);
                            }
                            if (isNotBlank(errMsg)) {
                                log.append(errMsg);
                                log.append(";");
                                logs.setHasError(true);
                            }
                            cellIndex++;
                        }
                    }
                    list.add(t);
                    logList.add(new ExcelLog(t, log.toString(), row.getRowNum() + 1));
                }
            }
            logs.setLogList(logList);
        } catch (InstantiationException e) {
            throw new RuntimeException(MessageFormat.format("can not instance class:{0}",
                    clazz.getSimpleName()), e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(MessageFormat.format("can not instance class:{0}",
                    clazz.getSimpleName()), e);
        }
        return list;
    }

    /**
     * 驗證Cell類型是否正確
     *
     * @param cell    cell單元格
     * @param field   欄位
     * @param cellNum 第幾個欄位,用於errMsg
     * @return
     */
    private static String validateCell(Cell cell, Field field, int cellNum) {
        String columnName = CellReference.convertNumToColString(cellNum);
        String result = null;
        CellType[] cellTypeArr = validateMap.get(field.getType());
        if (cellTypeArr == null) {
            result = MessageFormat.format("Unsupported type [{0}]", field.getType().getSimpleName());
            return result;
        }
        ExcelCell annoCell = field.getAnnotation(ExcelCell.class);
        if (cell == null
                || (cell.getCellType() == CellType.STRING && isBlank(cell
                .getStringCellValue()))) {
            if (annoCell != null && annoCell.valid().allowNull() == false) {
                result = MessageFormat.format("the cell [{0}] can not null", columnName);
            }
            ;
        } else if (cell.getCellType() == CellType.BLANK && annoCell.valid().allowNull()) {
            return result;
        } else {
            List<CellType> cellTypes = Arrays.asList(cellTypeArr);

            // 如果類型不在指定範圍內,並且沒有默認值
            if (!(cellTypes.contains(cell.getCellType()))
                    || isNotBlank(annoCell.defaultValue())
                    && cell.getCellType() == CellType.STRING) {
                StringBuilder strType = new StringBuilder();
                for (int i = 0; i < cellTypes.size(); i++) {
                    CellType cellType = cellTypes.get(i);
                    strType.append(getCellTypeByInt(cellType));
                    if (i != cellTypes.size() - 1) {
                        strType.append(",");
                    }
                }
                result =
                        MessageFormat.format("the cell [{0}] type must [{1}]", columnName, strType.toString());
            } else {
                // 類型符合驗證,但值不在要求範圍內的
                // String in
                if (annoCell.valid().in().length != 0 && cell.getCellType() == CellType.STRING) {
                    String[] in = annoCell.valid().in();
                    String cellValue = cell.getStringCellValue();
                    boolean isIn = false;
                    for (String str : in) {
                        if (str.equals(cellValue)) {
                            isIn = true;
                        }
                    }
                    if (!isIn) {
                        result = MessageFormat.format("the cell [{0}] value must in {1}", columnName, in);
                    }
                }
                // 數字型
                if (cell.getCellType() == CellType.NUMERIC) {
                    double cellValue = cell.getNumericCellValue();
                    // 小于
                    if (!Double.isNaN(annoCell.valid().lt())) {
                        if (!(cellValue < annoCell.valid().lt())) {
                            result =
                                    MessageFormat.format("the cell [{0}] value must less than [{1}]", columnName,
                                            annoCell.valid().lt());
                        }
                    }
                    // 大於
                    if (!Double.isNaN(annoCell.valid().gt())) {
                        if (!(cellValue > annoCell.valid().gt())) {
                            result =
                                    MessageFormat.format("the cell [{0}] value must greater than [{1}]", columnName,
                                            annoCell.valid().gt());
                        }
                    }
                    // 小於等於
                    if (!Double.isNaN(annoCell.valid().le())) {
                        if (!(cellValue <= annoCell.valid().le())) {
                            result =
                                    MessageFormat.format("the cell [{0}] value must less than or equal [{1}]",
                                            columnName, annoCell.valid().le());
                        }
                    }
                    // 大於等於
                    if (!Double.isNaN(annoCell.valid().ge())) {
                        if (!(cellValue >= annoCell.valid().ge())) {
                            result =
                                    MessageFormat.format("the cell [{0}] value must greater than or equal [{1}]",
                                            columnName, annoCell.valid().ge());
                        }
                    }
                }
            }
        }
        return result;
    }

    /**
     * 根據annotation的seq排序後的欄位
     *
     * @param clazz
     * @return
     */
    private static List<FieldForSortting> sortFieldByAnno(Class<?> clazz) {
        Field[] fieldsArr = clazz.getDeclaredFields();
        List<FieldForSortting> fields = new ArrayList<>();
        List<FieldForSortting> annoNullFields = new ArrayList<>();
        for (Field field : fieldsArr) {
            ExcelCell ec = field.getAnnotation(ExcelCell.class);
            if (ec == null) {
                // 沒有ExcelCell Annotation 視為不匯入
                continue;
            }
            int id = ec.index();
            fields.add(new FieldForSortting(field, id));
        }
        fields.addAll(annoNullFields);
        sortByProperties(fields, true, false, "index");
        return fields;
    }

    private static void sortByProperties(List<? extends Object> list, boolean isNullHigh,
                                         boolean isReversed, String... props) {
        if (CollectionUtils.isNotEmpty(list)) {
            Comparator<?> typeComp = ComparableComparator.getInstance();
            if (isNullHigh == true) {
                typeComp = ComparatorUtils.nullHighComparator(typeComp);
            } else {
                typeComp = ComparatorUtils.nullLowComparator(typeComp);
            }
            if (isReversed) {
                typeComp = ComparatorUtils.reversedComparator(typeComp);
            }

            List<Object> sortCols = new ArrayList<Object>();

            if (props != null) {
                for (String prop : props) {
                    sortCols.add(new BeanComparator(prop, typeComp));
                }
            }
            if (sortCols.size() > 0) {
                @SuppressWarnings("unchecked")
                Comparator<Object> sortChain = new ComparatorChain(sortCols);
                Collections.sort(list, sortChain);
            }
        }
    }

    private static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }
        return str.length() == 0;
    }

    protected static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

}
