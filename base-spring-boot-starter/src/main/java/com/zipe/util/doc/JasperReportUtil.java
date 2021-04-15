package com.zipe.util.doc;

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

/**
 * @author : Gary Tsai
 * @created : @Date 2021/1/6 下午 01:16
 **/
public class JasperReportUtil {

    public static byte[] exportPdfFile(String jrxmlFile, Map<String, Object> params) throws JRException {

        ClassLoader classLoader = JasperReportUtil.class.getClassLoader();
        InputStream inputStream = classLoader.getResourceAsStream(jrxmlFile);

        JasperReport jasperDesign = JasperCompileManager.compileReport(inputStream);
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperDesign, params, new JREmptyDataSource());

        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    public static void exportPdfFile(String jrxmlFile, File exportFile, Map<String, Object> params) throws IOException, JRException {
        OutputStream outputSteam = new FileOutputStream(exportFile);
        outputSteam.write(exportPdfFile(jrxmlFile, params));
    }
}
