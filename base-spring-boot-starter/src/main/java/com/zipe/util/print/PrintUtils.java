package com.zipe.util.print;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

/**
 * 列印工具類別，實作 {@link Printable} 介面，封裝 Java AWT 列印 API。
 * <p>
 * 提供多頁列印能力，使用者可透過 {@code contentMap} 以頁碼為鍵，
 * 指定每頁要輸出的文字內容、座標及字型；並可指定目標印表機名稱，
 * 若指定的印表機不存在則自動回退至系統預設印表機。
 * </p>
 * <p>
 * 典型使用流程：
 * <ol>
 *   <li>建立 {@code PrintUtils} 實例並設定 {@code printerName}、{@code jobName}、{@code contentMap}</li>
 *   <li>呼叫 {@link #initJob()} 初始化 {@link java.awt.print.PrinterJob}</li>
 *   <li>呼叫 {@link #doPrint()} 送出列印工作</li>
 * </ol>
 * </p>
 *
 * @author : Gary Tsai
 * @created : @Date 2021/1/25 下午 03:55
 **/
@Slf4j
public class PrintUtils implements Printable {

    /**
     * 列印名稱
     */
    private String jobName;
    /**
     * 設定印表機名稱
     */
    private String printerName;
    /**
     * key = 頁碼, value = 列印內容及字型大小風格設定
     */
    private Map<Integer, List<PrintContent>> contentMap;
    /**
     * 列印的紙張寬度，單位為 1/72 英寸（point）；初始為 0，於 {@link #initJob()} 中由印表機規格取得
     */
    private double paperW = 0;
    /**
     * 列印的紙張高度，單位為 1/72 英寸（point）；初始為 0，於 {@link #initJob()} 中由印表機規格取得
     */
    private double paperH = 0;
    /** 負責管理列印工作生命週期的 {@link PrinterJob} 實例，由 {@link #initJob()} 初始化 */
    private PrinterJob prnJob = null;

    /**
     * 預設建構子，使用者需在呼叫 {@link #initJob()} 前透過 setter 設定必要屬性。
     */
    public PrintUtils() {
    }

    /**
     * 實作 {@link Printable} 介面的列印回呼方法，由 {@link PrinterJob} 在每次需要繪製頁面時自動呼叫。
     *
     * @param graphics   由列印系統提供的 {@link Graphics} 繪圖物件，實際型別為 {@link Graphics2D}
     * @param pageFormat 目前頁面的版面格式（紙張大小、方向等）
     * @param pageIndex  目前正在列印的頁碼（從 0 開始），由系統自動遞增，無需手動維護
     * @return {@link Printable#PAGE_EXISTS} 表示頁面存在並已繪製；
     *         {@link Printable#NO_SUCH_PAGE} 表示已超出總頁數，通知系統停止列印
     * @throws PrinterException 列印過程發生錯誤時拋出
     */
    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
        if (pageIndex >= contentMap.size()) {
            //退出列印
            return Printable.NO_SUCH_PAGE;
        } else {
            Graphics2D g2 = (Graphics2D) graphics;
            g2.setColor(Color.BLUE);
            drawCurrentPageText(g2, pageIndex);//調用打印內容的方法
            return PAGE_EXISTS;
        }
    }

    /**
     * 將 {@code contentMap} 中指定頁碼的所有文字內容繪製至 {@link Graphics2D} 上下文。
     * <p>
     * 對每個 {@link PrintContent} 依序設定字體，並以其 x、y 座標在頁面上繪製文字；
     * 若 {@link PrintContent#getContent()} 為 {@code null}，以空字串代替，避免 NullPointerException。
     * </p>
     *
     * @param g2        由 {@link #print} 傳入的列印 {@link Graphics2D} 物件
     * @param pageIndex 目前頁碼（從 0 開始），用於從 {@code contentMap} 取得對應內容
     */
    private void drawCurrentPageText(Graphics2D g2, int pageIndex) {
        contentMap.get(pageIndex).forEach(content -> {
            g2.setFont(content.getFont());// 設置字體, 預設為 Font("標楷體", Font.BOLD, 12)
            // 表示從 pageFormat.getPaper() 中座標為(x,y)開始打印
            // 單位是1/72(inch)，inch:英寸
            g2.drawString(Optional.ofNullable(content.getContent()).orElse(""), content.getX(), content.getY());
        });
    }

    /**
     * 初始化列印工作物件（{@link PrinterJob}），設定紙張規格、頁面方向及目標印表機。
     * <p>
     * 若 {@code printerName} 對應的印表機不存在，自動改用系統預設印表機；
     * 若系統無任何可用印表機，則提早結束不執行後續設定。
     * 初始化完成後，{@code paperW} 與 {@code paperH} 會更新為實際印表機紙張規格。
     * </p>
     */
    public void initJob() {
        try {
            prnJob = PrinterJob.getPrinterJob();

            Paper p = new Paper();
            // paperW 與 paperH 取自目標印表機的進紙規格；實際針式印表機的可列印區域有限，
            // 距紙張上下左右各 1 英寸的矩形框才是真正可列印範圍，超出部分不會輸出（未設偏移時）。
            // 若手動指定的寬高與印表機進紙規格差距過大，系統將預設以 A4 紙為列印範本。
            p.setImageableArea(0, 0, paperW, paperH);// 設定可列印區域（起點 + 寬高）
            p.setSize(paperW, paperH);// 設定紙張大小


            PageFormat pageFormat = new PageFormat();
            pageFormat.setPaper(p);
            pageFormat.setOrientation(PageFormat.PORTRAIT);

            prnJob.setPrintable(this, pageFormat);
            prnJob.setJobName(Optional.ofNullable(jobName).orElse("Print"));

            // 如未指定印表機，則使用系統預設印表機
            PrintService service = Arrays.stream(PrinterJob.lookupPrintServices())
                    .filter(printer -> printer.getName().equals(printerName)).findFirst()
                    .orElse(PrintServiceLookup.lookupDefaultPrintService());
            if (ObjectUtils.isEmpty(service)) {
                return;
            }
            prnJob.setPrintService(service);

            // 取得已綁定印表機的實際進紙寬度，單位：1/72 英寸（point）
            paperW = prnJob.getPageFormat(null).getPaper().getWidth();
            // 取得已綁定印表機的實際進紙高度，單位：1/72 英寸（point）
            paperH = prnJob.getPageFormat(null).getPaper().getHeight();
//            prnJob.print();//啟動打印工作
        } catch (PrinterException ex) {
            ex.printStackTrace();
            log.error("打印錯誤：" + ex.toString());
        }
    }

    /**
     * 執行列印工作，將已配置的頁面內容送至印表機。
     * <p>
     * 須在呼叫 {@link #initJob()} 完成初始化後才能呼叫此方法。
     * 若 {@code prnJob} 尚未初始化（為 {@code null}），則不執行任何動作。
     * 列印失敗時僅記錄錯誤日誌，不向外拋出例外。
     * </p>
     *
     * @throws PrinterException 若列印工作送出過程發生錯誤時由 {@link java.awt.print.PrinterJob#print()} 拋出
     */
    public void doPrint() throws PrinterException {
        Optional.ofNullable(prnJob).ifPresent(job -> {
            try {
                job.print();//啟動列印工作
            } catch (PrinterException e) {
                log.error("啟動列印失敗");
            }
        });
    }

    /**
     * 取得列印工作名稱。
     *
     * @return 列印工作名稱；若未設定則於 {@link #initJob()} 中預設為 {@code "Print"}
     */
    public String getJobName() {
        return jobName;
    }

    /**
     * 設定列印工作名稱，會顯示於作業系統的列印佇列中。
     *
     * @param jobName 列印工作名稱
     */
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    /**
     * 取得目標印表機名稱。
     *
     * @return 印表機名稱；若為 {@code null} 或找不到對應印表機，將使用系統預設印表機
     */
    public String getPrinterName() {
        return printerName;
    }

    /**
     * 設定目標印表機名稱，須在呼叫 {@link #initJob()} 之前設定。
     *
     * @param printerName 印表機名稱（需與作業系統中的印表機名稱完全一致）
     */
    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    /**
     * 取得各頁列印內容對應表。
     *
     * @return 以頁碼（從 0 開始）為鍵、{@link PrintContent} 清單為值的 {@link Map}
     */
    public Map<Integer, List<PrintContent>> getContentMap() {
        return contentMap;
    }

    /**
     * 設定各頁列印內容對應表，須在呼叫 {@link #initJob()} 之前設定。
     *
     * @param contentMap 以頁碼（從 0 開始）為鍵、{@link PrintContent} 清單為值的 {@link Map}
     */
    public void setContentMap(Map<Integer, List<PrintContent>> contentMap) {
        this.contentMap = contentMap;
    }

//    //入口方法
//    public static void main(String[] args) {
//        PrintUtils pm = new PrintUtils();// 實例化打印類
////        pm.pageSize = 2;//打印兩頁
////        pm.starPrint();
//
//        Map<Integer, List<PrintContent>> contentMap = new HashMap<>();
//        List<PrintContent> contents = new ArrayList<>();
//        // main
//        contents.add(new PrintContent("test1", 100, 60));
//        contents.add(new PrintContent("test2", 100, 70));
//        contents.add(new PrintContent("test3", 130, 70));
//        contents.add(new PrintContent("test4", 450, 70));
//        contents.add(new PrintContent("test5", 100, 100));
//        contents.add(new PrintContent("test6", 140, 100));
//        contents.add(new PrintContent("test7", 100, 110));
//        contents.add(new PrintContent("test8", 100, 140));
//        contents.add(new PrintContent("test9", 200, 140));
//        contents.add(new PrintContent("test10", 400, 140));
//        contentMap.put(0, contents);
//        pm.setContentMap(contentMap);
//        pm.initJob();
//    }
}
