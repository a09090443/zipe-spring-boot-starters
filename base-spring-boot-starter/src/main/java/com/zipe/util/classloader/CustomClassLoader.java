package com.zipe.util.classloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.jar.JarFile;

/**
 * 自定義類載入器，可以從外部 JAR 載入類，並不會重複加載已經存在的類

 */
public class CustomClassLoader extends URLClassLoader {

    public CustomClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            //嘗試使用父類載入器載入類
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            // 如果父類載入器沒有載入該類，則嘗試從外部 JAR 載入
            return findClass(name);
        }
    }

    public void unloadJarFile(URL jarUrl) throws IOException {
        try {
            for (URL url : this.getURLs()) {
                if (url.equals(jarUrl)) {
                    JarFile jarFile = new JarFile(new File(url.getFile()));
                    jarFile.close();
                    break;
                }
            }
        } catch (Exception e) {
            throw new IOException("Failed to unload JAR file", e);
        }
    }

}
