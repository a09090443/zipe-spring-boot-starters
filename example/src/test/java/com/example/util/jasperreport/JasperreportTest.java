package com.example.util.jasperreport;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.junit.jupiter.api.Test;
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
    static final String outFile = "/home/zipe/tmp/Reports.pdf";

    @Test
    public void exportPdfTest() throws FileNotFoundException, JRException {

        log.info("generating jasper report...");

        // 1. compile template ".jrxml" file
        JasperReport jasperReport = getJasperReport();

        // 2. parameters "empty"
        Map<String, Object> parameters = getParameters();

        // 3. datasource "java object"
        JRDataSource dataSource = getDataSource();

        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport,
                parameters,
                dataSource);
        JasperExportManager.exportReportToPdfFile(jasperPrint, outFile);
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
