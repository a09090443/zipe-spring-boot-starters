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
 * Excel 檔案匯入／匯出工具類別。
 *
 * <p>提供以下主要功能：
 * <ul>
 *   <li>將 Java 集合（JavaBean 或 {@link java.util.Map}）匯出為 {@code .xls} / {@code .xlsx} 格式。</li>
 *   <li>將既有 Excel 檔案讀入並轉換為指定類型的物件集合。</li>
 *   <li>支援多 sheet、合併儲存格（Merge Cell）、儲存格樣式（顏色、粗體、對齊）等進階輸出功能。</li>
 * </ul>
 *
 * <p>此工具需搭配 {@link ExcelCell} Annotation 標記 JavaBean 欄位，以決定欄位順序與驗證規則。
 * 若欄位未標記 {@link ExcelCell}，該欄位將被忽略。
 */
@Slf4j
public class ExcelUtil {

    /**
     * 用來驗證 Excel 儲存格類型與 VO 欄位類型是否相容。<br>
     * key：欄位的 Java 型別；value：該型別允許對應的 {@link CellType} 陣列。
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
     * 取得 {@link CellType} 對應的文字描述，用於組合錯誤訊息。
     *
     * @param cellType POI 儲存格類型，可為
     *                 {@code CellType.BLANK}、{@code CellType.BOOLEAN}、{@code CellType.ERROR}、
     *                 {@code CellType.FORMULA}、{@code CellType.NUMERIC}、{@code CellType.STRING}
     * @return 對應的英文描述字串；若無符合類型則回傳 {@code "Unknown type"}
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
     * 取得 POI {@link Cell} 的實際值，並依照儲存格類型轉換為對應的 Java 物件。
     *
     * <p>若儲存格為 {@code null} 或字串值為空白，則直接回傳 {@code null}。
     * 日期格式的 NUMERIC/FORMULA 儲存格會回傳 {@link java.util.Date}。
     *
     * @param cell 要讀取的 POI 儲存格
     * @return 轉換後的 Java 物件（{@code Boolean}、{@code Double}、{@code Date}、{@code String} 等），
     *         若無法判斷則回傳 {@code null}
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
     * 依據列類型參數（{@code param}）設定 XSSF 儲存格樣式，包括字型顏色、背景色、對齊方式等。
     *
     * <p>支援的 {@code param} 值（對應 {@link com.zipe.util.string.StringConstant} 中的常數）：
     * <ul>
     *   <li>{@code PARAM_TITLE} / {@code PARAM_TITLEM}：標題列，綠色粗體字，可水平置中。</li>
     *   <li>{@code PARAM_CONTENT} / {@code PARAM_CONTENTM}：內容列，深灰背景。</li>
     *   <li>{@code PARAM_FOOTER} / {@code PARAM_FOOTERM}：頁尾列，深灰背景。</li>
     *   <li>{@code PARAM_HEADER} / {@code PARAM_HEADERM}：標頭列，綠底白字。</li>
     *   <li>{@code PARAM_BODY} / {@code PARAM_BODYM}：資料列，奇偶列交替淺灰背景（由 {@code num[0]} 控制）。</li>
     *   <li>{@code PARAM_TOTAL} / {@code PARAM_TOTALM}：合計列，首欄綠色粗體（由 {@code num[0]} 控制）。</li>
     * </ul>
     *
     * @param style 要設定的 {@link XSSFCellStyle} 物件
     * @param font  要設定的 {@link Font} 物件
     * @param param 列類型參數，決定套用的樣式規則
     * @param num   選用的數值參數，用於 BODY 奇偶列交替或 TOTAL 首欄粗體判斷
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
     * 判斷字串是否為整數（含正負號）。
     *
     * <p>使用正規表示式 {@code ^[-\+]?[\d]*$} 進行比對，用於解析合併儲存格的欄數或列數設定值。
     *
     * @param str 要判斷的字串
     * @return 若字串符合整數格式則回傳 {@code true}，否則回傳 {@code false}
     */
    private static boolean isInteger(String str) {
        Pattern pattern = Pattern.compile("^[-\\+]?[\\d]*$");
        return pattern.matcher(str).matches();
    }

    /**
     * 將資料集合匯出為 {@code .xlsx} 格式並寫入指定 {@link File}，支援將新 sheet 附加至既有活頁簿。
     *
     * <p>此方法透過解析 JavaBean 上的 {@link ExcelCell} Annotation 決定欄位順序與儲存格樣式。
     * 若資料元素為 {@link java.util.Map}，則直接依 key 順序輸出。
     * 支援合併儲存格（透過特殊常數字串控制）、奇偶列交替顏色等進階排版功能。
     *
     * @param <T>        資料集合的元素類型
     * @param sheetName  要建立的 sheet 名稱
     * @param dataset    要輸出的資料集合，元素可為 JavaBean 或 {@link java.util.Map}
     * @param file       目標輸出檔案
     * @param isAddSheet 若為 {@code true} 且 {@code file} 已存在，則將新 sheet 附加至既有活頁簿；
     *                   否則建立新活頁簿
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
        int mergeRows = -1;    // 目前待合併的跨列數（列方向），-1 表示無待合併
        int mergeColumns = -1; // 目前待合併的跨欄數（欄方向），-1 表示無待合併
        int mergeCount = 0;    // 已登記的合併區域總數，作為 merge Map 的索引
        int changeColarRows = 0;    // 奇偶列切換旗標（0/1），用於 BODY 列交替背景色
        int changeColarColumns = 0; // 奇偶欄切換旗標（0/1），用於 TOTAL 列首欄粗體
        int lastTime = 0;  // 記錄合併列的跨列數，供下一筆資料計算正確行號時使用
        int follow = 0;    // 0：目前儲存格為控制指令；1：目前儲存格為一般值
        // key：合併區域序號；value：{ 0:firstRow, 1:lastRow, 2:firstCol, 3:lastCol }
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
                                    // 若為跨欄合併指令（MERGE_COLUMNS），記錄跨欄數並跳過被合併的欄位
                                    if (kind.equals(StringConstant.PARAM_MERGE_COLUMNS) && datas.length > 2 && cellNum > 0) {
                                        if (isInteger(datas[2])) {
                                            if (mergeColumns == -1) {
                                                mergeColumns = Integer.valueOf(datas[2]);
                                                // 跳過後續被合併的欄，cellNum 直接前進
                                                cellNum += mergeColumns;
                                            }
                                            follow = 0;
                                            continue;
                                        }
                                    }
                                    // 若首欄（cellNum==0）帶有跨列數，記錄供後續行號計算
                                    if (datas.length > 2 && cellNum == 0) {
                                        if (isInteger(datas[2])) {
                                            mergeRows = Integer.valueOf(datas[2]);
                                            lastTime = mergeRows;
                                        }
                                    }
                                    // 首欄的控制指令決定本列的樣式類型
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

        // 將收集到的所有合併區域套用至 sheet
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
     * 將以 {@link Integer} 為索引鍵的多組標題與資料匯出為 {@code .xls} 格式（HSSFWorkbook）。
     *
     * <p>對應規則：
     * <ul>
     *   <li>{@code headers} 的 key 與 {@code dataset} 的 key 相同時，視為同一組資料。</li>
     *   <li>{@code headers} 有值但 {@code dataset} 無對應 key，表示只輸出標題列。</li>
     *   <li>{@code headers} 必須有值，可單獨存在；{@code dataset} 不可單獨存在，需搭配 {@code headers}。</li>
     * </ul>
     *
     * @param <T>     資料集合的元素類型
     * @param headers 標題結構，外層 key 為群組編號，內層為 {@code 欄位索引 -> 標題文字}
     * @param dataset 資料集合，key 對應 {@code headers} 的群組編號
     * @param out     目標輸出串流
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
     * 利用 Java 反射機制，將集合中的資料以 Excel 格式輸出至指定 {@link OutputStream}。<br>
     * 適用於單一 sheet，日期格式預設為 {@code "yyyy-MM-dd"}。
     *
     * @param <T>     資料集合的元素類型
     * @param headers 表格標題對照表，key 為欄位名稱，value 為顯示標題文字
     * @param dataset 要輸出的資料集合，元素須為符合 JavaBean 規範的物件；
     *                支援的欄位型別包含基本型別、{@link String}、{@link java.util.Date}、
     *                {@code String[]}、{@code Double[]}
     * @param out     目標輸出串流，可導出至本機檔案或網路回應
     */
    public static <T> void exportExcel(Map<String, String> headers, Collection<T> dataset, OutputStream out) {
        exportExcel(headers, dataset, out, null);
    }

    /**
     * 利用 Java 反射機制，將集合中的資料以 Excel 格式輸出至指定 {@link OutputStream}。<br>
     * 適用於單一 sheet，可自訂日期輸出格式。
     *
     * @param <T>     資料集合的元素類型
     * @param headers 表格標題對照表，key 為欄位名稱，value 為顯示標題文字
     * @param dataset 要輸出的資料集合，元素須為符合 JavaBean 規範的物件；
     *                支援的欄位型別包含基本型別、{@link String}、{@link java.util.Date}、
     *                {@code String[]}、{@code Double[]}
     * @param out     目標輸出串流，可導出至本機檔案或網路回應
     * @param pattern 日期格式字串；若為 {@code null} 或空白，預設使用 {@code "yyyy-MM-dd"}
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

    /**
     * 將二維字串陣列匯出為 {@code .xls} 格式並寫入指定 {@link OutputStream}。
     *
     * <p>每列（第一維）對應一個 Excel 資料列，每欄（第二維）對應一個儲存格。
     * 若儲存格字串長度超過 Excel 限制（32767），將截斷並附加提示文字。
     *
     * @param datalist        要輸出的二維字串資料，{@code datalist[i][j]} 為第 i 列第 j 欄的值
     * @param out             目標輸出串流
     * @param autoColumnWidth 若為 {@code true}，輸出後自動依內容調整各欄寬度
     */
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

    /**
     * 將二維字串陣列匯出為 {@code .xls} 格式並寫入指定 {@link OutputStream}，預設啟用自動欄寬。
     *
     * <p>等同於呼叫 {@link #exportExcel(String[][], OutputStream, boolean)} 並傳入 {@code true}。
     *
     * @param datalist 要輸出的二維字串資料
     * @param out      目標輸出串流
     */
    public static void exportExcel(String[][] datalist, OutputStream out) {
        exportExcel(datalist, out, true);
    }

    /**
     * 利用 Java 反射機制，將多個 {@link ExcelSheet} 的資料輸出至同一個 {@code .xls} 活頁簿。<br>
     * 適用於多個 sheet，日期格式預設為 {@code "yyyy-MM-dd"}。
     *
     * @param <T>    資料集合的元素類型
     * @param sheets 要輸出的 {@link ExcelSheet} 清單，每個元素對應一個 sheet
     * @param out    目標輸出串流，可導出至本機檔案或網路回應
     */
    public static <T> void exportExcel(List<ExcelSheet<T>> sheets, OutputStream out) {
        exportExcel(sheets, out, null);
    }

    /**
     * 利用 Java 反射機制，將多個 {@link ExcelSheet} 的資料輸出至同一個 {@code .xls} 活頁簿，並可自訂日期格式。<br>
     * 適用於多個 sheet。
     *
     * @param <T>     資料集合的元素類型
     * @param sheets  要輸出的 {@link ExcelSheet} 清單，每個元素對應一個 sheet
     * @param out     目標輸出串流，可導出至本機檔案或網路回應
     * @param pattern 日期格式字串；若為 {@code null} 或空白，預設使用 {@code "yyyy-MM-dd"}
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
     * 將標題與資料集合寫入單一 {@link HSSFSheet}。
     *
     * <p>第 0 列輸出標題列，後續列依序輸出資料；輸出完成後自動調整各欄寬度。
     *
     * @param <T>     資料集合的元素類型
     * @param sheet   要寫入的 {@link HSSFSheet} 物件
     * @param headers 標題對照表，key 為欄位名稱，value 為顯示標題文字
     * @param dataset 要輸出的資料集合
     * @param pattern 日期格式字串；若為 {@code null} 或空白，預設使用 {@code "yyyy-MM-dd"}
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

    /**
     * 依據 {@code value} 的實際型別，將值填入 {@link HSSFCell} 並回傳最終欄索引。
     *
     * <p>當 {@code value} 為 {@code String[]} 或 {@code Double[]} 時，陣列各元素會依序填入
     * 從 {@code cellNum} 開始的連續欄位，並回傳最後一個已填入欄的索引（不含 +1）。
     * 其他型別直接填入 {@code cell} 後回傳原 {@code cellNum}。
     *
     * @param cell    要填值的 {@link HSSFCell} 物件
     * @param value   要寫入的值，支援 {@link Integer}、{@link Float}、{@link Double}、{@link Long}、
     *                {@link Boolean}、{@link java.util.Date}、{@code String[]}、{@code Double[]} 及其他
     * @param pattern 日期格式字串，當 {@code value} 為 {@link java.util.Date} 時使用
     * @param cellNum 目前儲存格的欄索引
     * @param field   對應的 JavaBean 欄位，用於讀取 {@link ExcelCell} 的預設值（可為 {@code null}）
     * @param row     目前資料列，供陣列型別建立後續儲存格使用
     * @return 填值後最後一個已使用欄的索引
     */
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

    /**
     * 依據 {@code value} 的實際型別，將值填入 {@link XSSFCell} 並回傳最終欄索引。
     *
     * <p>與 {@link #setCellValue(HSSFCell, Object, String, int, Field, HSSFRow)} 邏輯相同，
     * 差異僅在於操作對象為 {@code .xlsx}（XSSF）格式的儲存格與列物件。
     *
     * @param cell    要填值的 {@link XSSFCell} 物件
     * @param value   要寫入的值，支援 {@link Integer}、{@link Float}、{@link Double}、{@link Long}、
     *                {@link Boolean}、{@link java.util.Date}、{@code String[]}、{@code Double[]} 及其他
     * @param pattern 日期格式字串，當 {@code value} 為 {@link java.util.Date} 時使用
     * @param cellNum 目前儲存格的欄索引
     * @param field   對應的 JavaBean 欄位，用於讀取 {@link ExcelCell} 的預設值（可為 {@code null}）
     * @param row     目前資料列，供陣列型別建立後續儲存格使用
     * @return 填值後最後一個已使用欄的索引
     */
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
     * 讀取 Excel 檔案（{@code .xls} 或 {@code .xlsx}），將資料列轉換為指定型別的物件集合。
     *
     * <p>第一列視為標題列，從第二列開始解析資料。
     * 若 {@code clazz} 為 {@link java.util.Map}{@code .class}，則以標題列文字為 key 建立 {@code Map<String, Object>}；
     * 否則依 {@link ExcelCell} Annotation 的 {@code index} 順序將欄位值對應至 JavaBean 屬性。
     *
     * @param <T>        目標物件類型
     * @param excelFile  要讀取的 Excel 檔案（必須存在且副檔名為 {@code xls} 或 {@code xlsx}）
     * @param clazz      目標物件的 {@link Class}，可為任意 JavaBean 或 {@link java.util.Map}{@code .class}
     * @param pattern    日期格式字串；當 Excel 儲存格為字串且欄位型別為 {@link java.util.Date} 時使用
     * @param logs       用於收集驗證錯誤的 {@link ExcelLogs} 物件，解析過程中遇到錯誤會寫入此集合
     * @param arrayCount 若 JavaBean 中有陣列型別欄位，依欄位出現順序依序傳入各陣列應包含的元素個數
     * @return 解析成功的物件集合；若檔案讀取失敗則回傳 {@code null}
     * @throws RuntimeException 若目標類別無法實例化（缺少無參建構子或存取權限不足）
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
     * 驗證 {@link Cell} 的類型與值是否符合 JavaBean 欄位的 {@link ExcelCell} 規範。
     *
     * <p>依序檢查：
     * <ol>
     *   <li>是否允許為空（{@code allowNull}）</li>
     *   <li>儲存格類型是否在允許範圍內</li>
     *   <li>字串值是否在允許的列舉清單（{@code in}）中</li>
     *   <li>數值是否符合大小比較條件（{@code lt}、{@code gt}、{@code le}、{@code ge}）</li>
     * </ol>
     *
     * @param cell    要驗證的 {@link Cell} 物件
     * @param field   對應的 JavaBean 欄位（含 {@link ExcelCell} Annotation）
     * @param cellNum 欄索引，用於組合錯誤訊息中的欄名（如 {@code "A"}、{@code "B"}）
     * @return 若驗證通過回傳 {@code null}；否則回傳描述錯誤原因的訊息字串
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
     * 依據 {@link ExcelCell#index()} 排序，回傳指定類別中所有標記 {@link ExcelCell} 的欄位清單。
     *
     * <p>未標記 {@link ExcelCell} 的欄位會被忽略，不納入結果。
     *
     * @param clazz 要解析的 JavaBean 類別
     * @return 依 {@code index} 由小到大排序的 {@link FieldForSortting} 清單
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

    /**
     * 依指定屬性名稱對清單進行原地排序（in-place sort）。
     *
     * <p>使用 Apache Commons 的 {@link BeanComparator} 讀取物件屬性值，並依序組合為多鍵排序鏈。
     * 可控制 {@code null} 值排在最前或最後，以及排序方向。
     *
     * @param list       要排序的清單；為空或 {@code null} 時不執行任何操作
     * @param isNullHigh 若為 {@code true}，{@code null} 值排在最後（高位）；否則排在最前（低位）
     * @param isReversed 若為 {@code true}，採用反向（由大到小）排序
     * @param props      要比對的 JavaBean 屬性名稱，依序作為多鍵排序依據
     */
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

    /**
     * 判斷字串是否為 {@code null} 或長度為 0 的空字串。
     *
     * @param str 要判斷的字串
     * @return 若 {@code str} 為 {@code null} 或空字串則回傳 {@code true}，否則回傳 {@code false}
     */
    private static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }
        return str.length() == 0;
    }

    /**
     * 判斷字串是否不為 {@code null} 且長度大於 0。
     *
     * @param str 要判斷的字串
     * @return 若 {@code str} 不為 {@code null} 且不為空字串則回傳 {@code true}，否則回傳 {@code false}
     */
    protected static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

}
