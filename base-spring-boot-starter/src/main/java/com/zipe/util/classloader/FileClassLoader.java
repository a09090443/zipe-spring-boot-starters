package com.zipe.util.classloader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;

/**
 * @Description 讀取並執行指定的 Class file
 */
public class FileClassLoader extends ClassLoader {

    private String classPath;

    public FileClassLoader() {
    }

    public FileClassLoader(String classPath) {
        this.classPath = classPath;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
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

    public Class<?> findClass(String name, FileInputStream fis) throws IOException {
        byte[] b = loadClassData(fis);
        return defineClass(name, b, 0, b.length);
    }

    private String getFileName(String name) {
        int index = name.lastIndexOf(".");
        if (index == -1) {
            return name + ".class";
        } else {
            return name.substring(index + 1) + ".class";
        }
    }

    private byte[] loadClassData(FileInputStream fis) throws IOException {
        byte[] buffer = new byte[fis.available()];
        int bytesRead = fis.read(buffer);
        if (bytesRead != buffer.length) {
            throw new IOException("Could not read the entire file: " + bytesRead + " bytes read, but expected " + buffer.length);
        }
        return buffer;
    }

    public static void main(String[] args) throws MalformedURLException {

        FileClassLoader diskClassLoader = new FileClassLoader("D:/tmp/");
        try {
            Class<?> c = diskClassLoader.loadClass("tw.com.webcomm.util.WebServiceHandler");
            if (c != null) {
                Object obj = c.getDeclaredConstructor().newInstance();
                Method method = c.getDeclaredMethod("test", String.class);
                method.invoke(obj, "I'm back");
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | NoSuchMethodException |
                 InvocationTargetException e) {
            throw new RuntimeException(e);
        }

    }
}
