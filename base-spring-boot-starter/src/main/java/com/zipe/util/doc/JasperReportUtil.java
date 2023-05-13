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
 * @author : Gary Tsai
 * @created : @Date 2021/1/6 下午 01:16
 **/
public class JasperReportUtil {

  public static byte[] exportPdfFile(String jrxmlFile, Map<String, Object> params,
      JRDataSource dataSource) throws JRException, FileNotFoundException {

    File template = ResourceUtils.getFile(jrxmlFile);

    JasperReport jasperDesign = JasperCompileManager.compileReport(template.getAbsolutePath());
    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperDesign, params,
        Objects.nonNull(dataSource) ? dataSource : new JREmptyDataSource());

    return JasperExportManager.exportReportToPdf(jasperPrint);
  }

  public static void exportPdfFile(String jrxmlFile, File exportFile, Map<String, Object> params)
      throws IOException, JRException {
    OutputStream outputSteam = new FileOutputStream(exportFile);
    outputSteam.write(exportPdfFile(jrxmlFile, params, null));
  }

  public static void exportPdfFile(String jrxmlFile, File exportFile, Map<String, Object> params,
      JRDataSource dataSource) throws IOException, JRException {
    OutputStream outputSteam = new FileOutputStream(exportFile);
    outputSteam.write(exportPdfFile(jrxmlFile, params, dataSource));
  }

}
