package com.zipe.util.print;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
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

/**
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
     * 打印的紙張寬度
     */
    private double paperW = 0;
    /**
     * 打印的紙張高度
     */
    private double paperH = 0;
    private PrinterJob prnJob = null;

    public PrintUtils() {
    }

    /**
     * 實現java.awt.print.Printable接口的打印方法
     *
     * @param graphics
     * @param pageFormat
     * @param pageIndex  打印的當前頁，此參數是系統自動維護的，不需要手動維護，系統會自動遞增
     * @return
     * @throws PrinterException
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
     * 打印內容
     *
     * @param g2
     * @param pageIndex
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
     * 初始 Print job object
     */
    public void initJob() {
        try {
            prnJob = PrinterJob.getPrinterJob();

            Paper p = new Paper();
            //此處的paperW和paperH是從目標打印機的進紙規格中獲取的，實際針式打印機的可打印區域是有限的，
            //距紙張的上下左右1inch(英寸)的中間的距形框為實際可打印區域，超出範圍的內容將不會打印出來(沒有設置偏移的情況)
            //如果設置偏移量，那麽超出的範圍也是可以打印的，這裏的pageW和pageH我是直接獲取打印機的進紙規格的寬和高
            //也可以手動指定，從是如果手動指定的寬高和目標打印機的進紙規格相差較大，將會默認以A4紙為打印模版
            p.setImageableArea(0, 0, paperW, paperH);// 設置可打印區域
            p.setSize(paperW, paperH);// 設置紙張的大小


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

            //獲取所連接的目標打印機的進紙規格的寬度，單位：1/72(inch)
            paperW = prnJob.getPageFormat(null).getPaper().getWidth();
            //獲取所連接的目標打印機的進紙規格的寬度，單位：1/72(inch)
            paperH = prnJob.getPageFormat(null).getPaper().getHeight();
//            prnJob.print();//啟動打印工作
        } catch (PrinterException ex) {
            ex.printStackTrace();
            log.error("打印錯誤：" + ex.toString());
        }
    }

    /**
     * 執行列印工作
     *
     * @throws PrinterException
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

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getPrinterName() {
        return printerName;
    }

    public void setPrinterName(String printerName) {
        this.printerName = printerName;
    }

    public Map<Integer, List<PrintContent>> getContentMap() {
        return contentMap;
    }

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
