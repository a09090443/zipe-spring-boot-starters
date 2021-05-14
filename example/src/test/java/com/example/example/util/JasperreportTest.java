package com.example.example.util;

import com.zipe.util.string.RandomUtil;
import com.zipe.util.validation.Validation;
import net.sf.jasperreports.engine.JREmptyDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/12/28 下午 03:13
 **/
public class JasperreportTest {
    static final String fileName = "src/jasperreport/JasperDesign.jrxml";
    static final String outFile = "D:/tmp/Reports.pdf";
    @Test
    public void exportPdfTest() throws FileNotFoundException, JRException {

//        System.out.println(Validation.isSTR_NUM("04123481".trim()));
//        if(!Validation.isSTR_NUM("04123481".trim()) || "04123481".trim().equals(RandomUtil.generateZeroStr(8))){
//            System.out.println("ttttt");
//        }
//        System.out.println(Validation.isINTEGER_NEGATIVE("3242352356222   ".trim()));
//
//
//        System.out.println("3242352356222   ".trim().equals(RandomUtil.generateZeroStr(8)));
//        InvoiceMoneySummary c0401Summary = new InvoiceMoneySummary();
//        InvoiceMoneySummary d0401Summary = new InvoiceMoneySummary();
//        c0401Summary.setType("AAAA");
//        d0401Summary.setType("BBBB");
//        Map<String, Object> parameter  = new HashMap<String, Object>();
//
//        parameter.put("year", "109");
//        parameter.put("period", "11-12");
//        parameter.put("companyId", "84703052");
//        parameter.put("companyName", "遠雄人壽");
//        parameter.put("c0401Summary", c0401Summary);
//        parameter.put("d0401Summary", d0401Summary);
//
//        parameter.put("title", new String("Hi, I am Title"));
//        ClassLoader classLoader = getClass().getClassLoader();
//        InputStream inputStream = classLoader.getResourceAsStream("jasperreport/InvoiceSummaryReport.jrxml.old");
////        File filer = new File(classLoader.getResource("jasperreport/JasperDesign.jrxml").getFile());
//
//        JasperReport jasperDesign = JasperCompileManager.compileReport(inputStream);
//        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperDesign, parameter,
//                new JREmptyDataSource());
//
//        File file = new File(outFile);
//        OutputStream outputSteam = new FileOutputStream(file);
//        JasperExportManager.exportReportToPdfStream(jasperPrint, outputSteam);
//
//        System.out.println("Report Generated!");
    }
}
