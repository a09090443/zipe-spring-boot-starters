package com.zipe.util.doc;

import java.io.FileNotFoundException;
import java.util.Objects;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;
import org.springframework.util.ResourceUtils;

/**
 * JasperReport 報表工具類別。
 *
 * <p>封裝 JasperReports 引擎的編譯、填充與匯出流程，提供將 {@code .jrxml} 報表範本
 * 轉換為 PDF 的靜態工具方法。支援以位元組陣列回傳，或直接寫出至指定 {@link File}。</p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/1/6 下午 01:16
 **/
public class JasperReportUtil {

  /**
   * 將指定的 {@code .jrxml} 報表範本編譯、填充並匯出為 PDF 位元組陣列。
   *
   * <p>若傳入的 {@code dataSource} 為 {@code null}，則自動使用
   * {@link JREmptyDataSource} 作為空資料來源，以確保填充流程正常執行。</p>
   *
   * @param jrxmlFile  報表範本的路徑（支援 Spring {@code ResourceUtils} 可解析的路徑格式，
   *                   例如 {@code classpath:reports/sample.jrxml}）
   * @param params     報表參數 Map，鍵為參數名稱，值為對應的物件
   * @param dataSource 報表資料來源；若無資料列需求可傳入 {@code null}
   * @return 匯出後的 PDF 內容位元組陣列
   * @throws JRException           JasperReports 編譯或填充過程發生錯誤時拋出
   * @throws FileNotFoundException 找不到指定的報表範本檔案時拋出
   */
  public static byte[] exportPdfFile(String jrxmlFile, Map<String, Object> params,
      JRDataSource dataSource) throws JRException, FileNotFoundException {

    File template = ResourceUtils.getFile(jrxmlFile);

    // 編譯 .jrxml 範本為 JasperReport 物件（需要完整絕對路徑）
    JasperReport jasperDesign = JasperCompileManager.compileReport(template.getAbsolutePath());
    // 填充報表：若呼叫端未提供資料來源，以空資料來源代替以避免 NullPointerException
    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperDesign, params,
        Objects.nonNull(dataSource) ? dataSource : new JREmptyDataSource());

    return JasperExportManager.exportReportToPdf(jasperPrint);
  }

  /**
   * 將指定的 {@code .jrxml} 報表範本匯出為 PDF，並寫入至指定的 {@link File}。
   *
   * <p>此多載不接受自訂資料來源，內部以 {@code null} 傳入，
   * 由 {@link #exportPdfFile(String, Map, JRDataSource)} 自動替換為空資料來源。</p>
   *
   * @param jrxmlFile  報表範本的路徑
   * @param exportFile 輸出目標檔案
   * @param params     報表參數 Map
   * @throws IOException  寫入輸出檔案時發生 I/O 錯誤
   * @throws JRException  JasperReports 處理過程發生錯誤時拋出
   */
  public static void exportPdfFile(String jrxmlFile, File exportFile, Map<String, Object> params)
      throws IOException, JRException {
    try (OutputStream outputStream = new FileOutputStream(exportFile)) {
      outputStream.write(exportPdfFile(jrxmlFile, params, null));
    }
  }

  /**
   * 將指定的 {@code .jrxml} 報表範本以自訂資料來源匯出為 PDF，並寫入至指定的 {@link File}。
   *
   * @param jrxmlFile  報表範本的路徑
   * @param exportFile 輸出目標檔案
   * @param params     報表參數 Map
   * @param dataSource 報表資料來源；若無資料列需求可傳入 {@code null}
   * @throws IOException  寫入輸出檔案時發生 I/O 錯誤
   * @throws JRException  JasperReports 處理過程發生錯誤時拋出
   */
  public static void exportPdfFile(String jrxmlFile, File exportFile, Map<String, Object> params,
      JRDataSource dataSource) throws IOException, JRException {
    try (OutputStream outputStream = new FileOutputStream(exportFile)) {
      outputStream.write(exportPdfFile(jrxmlFile, params, dataSource));
    }
  }

}
