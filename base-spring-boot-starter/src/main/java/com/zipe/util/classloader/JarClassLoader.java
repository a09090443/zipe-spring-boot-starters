package com.zipe.util.classloader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/**
 * @Description 讀取並執行指定的 Jar file
 */
public class JarClassLoader extends URLClassLoader {
    private List<String> appliedPackages;

    public JarClassLoader(URL[] urls) {
        super(urls);
    }

    private List<String> getAppliedPackages() {
        if (appliedPackages == null) {
            this.appliedPackages = new ArrayList<String>();
        }
        return this.appliedPackages;
    }

    public void addAppliedPackages(String packageName) {
        getAppliedPackages().add(packageName);
    }

    private boolean applyFixedLoad(String className) {
        for (String packageName : getAppliedPackages()) {
            if (className.contains(packageName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        if (applyFixedLoad(name)) {
            // construct path with .class file
            String classFile = name.replaceAll("\\.", "/") + ".class";
            // Read all entries of defined JARs and try to found given class
            URL[] urls = getURLs();
            int i = 0;
            Class<?> searchedClass = null;
            while (searchedClass == null && i < urls.length) {
                URL url = urls[i];
                i++;
                JarFile jarFile = null;
                try {
                    /**
                     * JarFile is constructed directly from File instance. After
                     * we get ZipEntry which is, in fact, the element composing the JAR.
                     * If entry is found, we construct class instance from read bytes.
                     * The bytes are read in readClassBytesFromFile method.
                     */
                    File jar = new File(url.getFile());
                    try {
                        jarFile = new JarFile(jar);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    ZipEntry entry = jarFile.getEntry(classFile);
                    if (entry != null) {
                        byte[] entryBytes = readClassBytesFromFile(jar, classFile);
                        searchedClass = defineClass(name, entryBytes, 0, entryBytes.length);
                    }
                } finally {
                    if (jarFile != null) {
                        try {
                            jarFile.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
            return searchedClass;
        }
        return super.findClass(name);
    }

    /**
     * Reads bytes from given JAR file. The reading is based on special URL implementation
     * dedicated to JARs access. It starts by jar: protocol and continues with the name of the file.
     * The final !/ separates this two parts from the name of searched class. For example,
     * this URL jar:file://home/bartosz/tmp/jars/spring-core-4.0.0.RELEASE.jar!/org/springframework/util/PropertyPlaceholderHelper.class
     * - jar: marks the URL as JAR URL
     * - file://home/bartosz/tmp/jars/spring-core-4.0.0.RELEASE.jar: address of the JAR file containing searched class
     * - !/org/springframework/util/PropertyPlaceholderHelper.class: the name of searched class
     *
     * @param jar       File containing searched class.
     * @param className Searched class.
     * @return Array of bytes with searched class (if error, the array is empty).
     */
    private byte[] readClassBytesFromFile(File jar, String className) {
        InputStream stream = null;
        try {
            URL urlTmp = new URL("jar:" + getURI(jar) + "!/" + className);
            JarURLConnection jarUrl = (JarURLConnection) urlTmp.openConnection();
            stream = jarUrl.getInputStream();
            byte[] bytes = new byte[stream.available()];
            stream.read(bytes);
            return bytes;
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        if (applyFixedLoad(name)) {
            Class<?> searchedClass = findClass(name);
            if (resolve) {
                resolveClass(searchedClass);
            }
            return searchedClass;
        }
        return super.loadClass(name, resolve);
    }


    /**
     * Taken from Tomcat7 class loader: org.apache.catalina.loader.WebappClassLoader
     */
    private URL getURI(File file) throws MalformedURLException, IOException {
        File realFile = file;
        realFile = realFile.getCanonicalFile();
        return realFile.toURI().toURL();
    }

    public static void main(String[] args) throws MalformedURLException, ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        JarClassLoader loader = new JarClassLoader(new URL[]{
                new URL("file:D:\\tmp\\classloaderTest.jar")
        });
        loader.addAppliedPackages("org.example");
        String searchedClass = "org.example.Test2";
        Class<?> c = loader.loadClass(searchedClass);
        if (c != null) {
            Object obj = c.getDeclaredConstructor().newInstance();
            Method method = c.getDeclaredMethod("getValue", String.class);
            method.invoke(obj, "3213");
        }
    }
}
