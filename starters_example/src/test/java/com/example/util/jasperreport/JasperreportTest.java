package com.example.util.jasperreport;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zipe.util.doc.JasperReportUtil;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @author : Gary Tsai
 **/
@Slf4j
public class JasperreportTest {

  /** PDF 輸出至 JUnit 提供的暫存目錄，避免硬編 d:\tmp 並可跨平台執行。 */
  @TempDir
  Path tempDir;

  @Test
  void exportPdfUserUtil() throws JRException, IOException {
    log.info("generating pdf file using util...");

    File output = tempDir.resolve("SimpleReporter.pdf").toFile();
    SimpleBeanMaker simpleBeanMaker = new SimpleBeanMaker();
    ArrayList<SimpleBean> simpleBeanList = simpleBeanMaker.getDataBeanList();
    Map<String, Object> parameters = new HashMap<>();
    JRBeanCollectionDataSource beanColDataSource = new JRBeanCollectionDataSource(simpleBeanList);
    JasperReportUtil.exportPdfFile("classpath:jasperreport/SimpleReporter.jrxml", output,
        parameters, beanColDataSource);

    assertTrue(output.exists() && output.length() > 0, "JasperReportUtil 應產生非空 PDF 檔");
  }

  @Test
  void exportPdfTest() throws FileNotFoundException, JRException {

    log.info("generating pdf file...");
    File outFile = tempDir.resolve("ExampleReport.pdf").toFile();
    // 1. compile template ".jrxml" file
    JasperReport jasperReport = getJasperReport();

    // 2. parameters "empty"
    Map<String, Object> parameters = getParameters();

    // 3. datasource "java object"
    JRDataSource dataSource = getDataSource();

    JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,
        parameters,
        dataSource);
    JasperExportManager.exportReportToPdfFile(jasperPrint, outFile.getAbsolutePath());

    assertTrue(outFile.exists() && outFile.length() > 0, "JasperExportManager 應產生非空 PDF 檔");
  }


  private static JasperReport getJasperReport() throws FileNotFoundException, JRException {
    File template = ResourceUtils.getFile("classpath:jasperreport/ExampleReport.jrxml");
    return JasperCompileManager.compileReport(template.getAbsolutePath());
  }

  private static Map<String, Object> getParameters() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put("createdBy", "Gary");
    return parameters;
  }

  private static JRDataSource getDataSource() {

    List<Country> countries = new LinkedList<>();

    countries.add(new Country("IS", "Iceland",
        "https://i.pinimg.com/originals/72/b4/49/72b44927f220151547493e528a332173.png"));

    // 9 other countries in GITHUB

    return new JRBeanCollectionDataSource(countries);
  }
}
