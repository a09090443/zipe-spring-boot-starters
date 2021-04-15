package com.zipe.util;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;

import javax.servlet.http.HttpServletRequest;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;

/**
 * @author : Gary Tsai
 * @created : @Date 2020/11/24 下午 05:39
 **/
public final class VelocityUtil {
    private VelocityEngine ve;
    private HttpServletRequest request;
    private String resourcePath;
    private String dir;

    public VelocityUtil() {
    }

    public void initClassPath() {
        Properties p = new Properties();
        p.put("resource.loader", "classpath");
        p.put("classpath.resource.loader.class", ClasspathResourceLoader.class.getName());
        this.init(p);
    }

    public void initFilePath() {
        Properties p = new Properties();
        p.put("resource.loader", "file");
        p.put("file.resource.loader.class", FileResourceLoader.class.getName());
        this.init(p);
    }

    public void initFileSystemPath(String templateFilePath) {
        Properties p = new Properties();
        p.put("resource.loader", "file");
        if (null != templateFilePath && !"".equals(templateFilePath)) {
            p.put("file.resource.loader.path", templateFilePath);
        } else {
            p.put("file.resource.loader.path", null != this.resourcePath && !"".equals(this.resourcePath) ? this.resourcePath : "tempalte/");
        }

        this.init(p);
    }

    public void initWebPath(HttpServletRequest request, String templateFilePath) {
        this.request = request;
        Properties p = new Properties();
        p.put("resource.loader", "webapp");
        p.put("webapp.resource.loader.class", "org.apache.velocity.tools.view.servlet.WebappLoader");
        if (null != templateFilePath && !"".equals(templateFilePath)) {
            p.put("webapp.resource.loader.path", templateFilePath);
        } else {
            p.put("webapp.resource.loader.path", null != this.resourcePath && !"".equals(this.resourcePath) ? this.resourcePath : "/WEB-INF/template/");
        }

        this.init(p);
    }

    public void writeTemplateOutput(String templateName, String outputFile, Map<String, Object> map) throws Exception {
        try {
            BufferedWriter bw = null;

            try {
                bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile), StandardCharsets.UTF_8.name()), 1048576);
                bw.write(this.generateContect(templateName, map));
                bw.flush();
            } catch (Exception var10) {
                throw var10;
            } finally {
                if (bw != null) {
                    bw.close();
                }

            }

        } catch (Exception var12) {
            throw var12;
        }
    }

    public String generateContect(String templateName, Map<String, Object> map) {
        StringWriter writer = new StringWriter();
        if (null != this.dir && "" != this.dir) {
            templateName = this.dir + File.separator + templateName;
        }

        try {
            Template template = this.ve.getTemplate(templateName);
            VelocityContext context = new VelocityContext();
            Set<String> keys = map.keySet();
            Iterator var7 = keys.iterator();

            while (var7.hasNext()) {
                String key = (String) var7.next();
                context.put(key, map.get(key));
            }

            template.merge(context, writer);
            return writer.toString();
        } catch (Exception var9) {
            throw var9;
        }
    }

    private void init(Properties p) {
        p.put("input.encoding", StandardCharsets.UTF_8.name());
        p.put("output.encoding", StandardCharsets.UTF_8.name());
        if (null == this.ve) {
            this.ve = new VelocityEngine(p);
            if (!Objects.isNull(this.request)) {
                this.ve.setApplicationAttribute("javax.servlet.ServletContext", this.request.getSession().getServletContext());
            }

            this.ve.init();
        }

    }

    public void close() {
        if (null != this.ve) {
            this.ve = null;
        }

    }

    public static void main(String[] args) {
//        VelocityUtil createHtml = new VelocityUtil();
//        Map<String, Object> map = new HashMap();
//        map.put("name", "Gary Tsai");
//
//        try {
//            createHtml.writeTemplateOutput("template/hello.vm", "/home/zipe/tmp/test.html", map);
//        } catch (Exception var4) {
//            var4.printStackTrace();
//        }

    }

    public String getResourcePath() {
        return this.resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getDir() {
        return this.dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }
}
