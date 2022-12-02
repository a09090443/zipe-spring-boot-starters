package com.zipe.util.classloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @Description 讀取並執行指定的 Class file
 */
public class FileClassLoader extends ClassLoader {

    private String classPath;

    public FileClassLoader(String classPath) {
        this.classPath = classPath;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        String fileName = getFileName(name);
        File file = new File(classPath, fileName);
        try {
            FileInputStream is = new FileInputStream(file);
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            int len = 0;
            try {
                while ((len = is.read()) != -1) {
                    bos.write(len);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            byte[] data = bos.toByteArray();
            is.close();
            bos.close();
            return defineClass(name, data, 0, data.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return super.findClass(name);
    }

    private String getFileName(String name) {
        int index = name.lastIndexOf(".");
        if (index == -1) {
            return name + ".class";
        } else {
            return name.substring(index + 1) + ".class";
        }
    }

    public static void main(String[] args) throws MalformedURLException {

        File file = new File("D:\\tmp\\classloaderTest.jar");
        URL url = file.toURI().toURL();

        URLClassLoader classLoader = (URLClassLoader)ClassLoader.getSystemClassLoader();

        FileClassLoader diskClassLoader = new FileClassLoader("D:\\tmp");
        try {
            Class<?> c = diskClassLoader.loadClass("org.example.Test1");
            if (c != null) {
                Object obj = c.getDeclaredConstructor().newInstance();
                Method method = c.getDeclaredMethod("callback", String.class);
                method.invoke(obj, "I'm back");
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }
}
