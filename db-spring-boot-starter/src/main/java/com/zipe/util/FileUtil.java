package com.zipe.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class FileUtil {
    /***************************************************************
     * 檔案比較
     */
    /**
     * @param file1       檔案1
     * @param file2       檔案2
     * @param charsetName 檔案編碼，可以為空
     * @return 是否相同
     * @throws IOException
     * @方法名：contentEquals
     * @方法描述【方法功能描述】 判斷兩個檔案是否相同
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 上午10:02:17
     * @修改人：cc
     * @修改時間：2018年8月28日 上午10:02:17
     */
    public static boolean contentEquals(File file1, File file2, String charsetName) throws IOException {
        boolean check = false;
        if (StringUtils.isBlank(charsetName)) {
            check = FileUtils.contentEquals(file1, file2);
        } else {
            check = FileUtils.contentEqualsIgnoreEOL(file1, file2, charsetName);
        }
        return check;
    }

    /**
     * @param file 檔案
     * @param date 比較日期
     * @return 檔案建立日期早則為false，晚為true
     * @throws IOException
     * @方法名：isFileNewer
     * @方法描述【方法功能描述】判斷檔案的建立日期與日期比較，檔案建立日期早則為false，晚為true
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 上午11:23:03
     * @修改人：cc
     * @修改時間：2018年8月28日 上午11:23:03
     */
    public static boolean isFileNewer(File file, Date date) throws IOException {
        return FileUtils.isFileNewer(file, date);
    }

    /***************************************************************
     * 檔案查詢
     */
    /**
     * @param directory  目錄
     * @param recursive  是否遞迴
     * @param fileFilter 檔案過濾器
     * @param dirFilter  目錄過濾器
     * @return 檔案列表
     * @方法名：listFiles
     * @方法描述【方法功能描述】 查詢目錄下檔案
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 下午1:42:37
     * @修改人：cc
     * @修改時間：2018年8月28日 下午1:42:37
     */
    public static List<File> listFiles(File directory, boolean recursive, IOFileFilter fileFilter,
                                       IOFileFilter dirFilter) throws IOException {
        if (recursive) {
            return (List<File>) FileUtils.listFiles(directory, fileFilter, dirFilter);
        } else {
            return (List<File>) FileUtils.listFilesAndDirs(directory, fileFilter, dirFilter);
        }
    }

    /**
     * @param file     檔案
     * @param encoding 編碼
     * @return 檔案字串
     * @throws IOException
     * @方法名：readFileToString
     * @方法描述【方法功能描述】讀取檔案為字串
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 下午1:47:20
     * @修改人：cc
     * @修改時間：2018年8月28日 下午1:47:20
     */
    public static String readFileToString(File file, String encoding) throws IOException {
        return FileUtils.readFileToString(file, encoding);
    }

    /**
     * @param file     檔案
     * @param encoding 編碼
     * @return 多行字串
     * @throws IOException
     * @方法名：readLines
     * @方法描述【方法功能描述】讀取檔案為多行字串
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 下午1:48:32
     * @修改人：cc
     * @修改時間：2018年8月28日 下午1:48:32
     */
    public static List<String> readLines(File file, String encoding) throws IOException {
        return FileUtils.readLines(file, encoding);
    }

    /**
     * @param file 檔案/目錄
     * @return 大小
     * @throws IOException
     * @方法名：sizeOf
     * @方法描述【方法功能描述】查詢檔案大小
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 下午1:55:40
     * @修改人：cc
     * @修改時間：2018年8月28日 下午1:55:40
     */
    public static Long sizeOf(File file) throws IOException {
        if (file.isDirectory()) {
            return FileUtils.sizeOf(file);
        } else {
            return FileUtils.sizeOfDirectory(file);
        }

    }

    /***************************************************************
     * 檔案新增
     */
    /**
     * @param file     檔案
     * @param data     字串
     * @param encoding 編碼
     * @param append   是否追加,true則追加，false則覆蓋
     * @throws IOException
     * @方法名：writeStringToFile
     * @方法描述【方法功能描述】 將一個字串寫入一個檔案建立檔案，如果不存在。
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 下午2:05:01
     * @修改人：cc
     * @修改時間：2018年8月28日 下午2:05:01
     */
    public static void writeStringToFile(File file, String data, String encoding, boolean append) throws IOException {
        FileUtils.writeStringToFile(file, data, encoding, append);
    }

    /**
     * @param file       檔案
     * @param encoding   編碼
     * @param lines      多行文字
     * @param lineEnding 要使用的行分隔符null是系統預設值
     * @param append     是否追加,true則追加，false則覆蓋
     * @throws IOException
     * @方法名：writeLines
     * @方法描述【方法功能描述】將多行文字寫入檔案
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 下午2:07:39
     * @修改人：cc
     * @修改時間：2018年8月28日 下午2:07:39
     */
    public static void writeLines(File file, String encoding, List<String> lines, String lineEnding, boolean append)
            throws IOException {
        if (StringUtils.isBlank(lineEnding)) {
            FileUtils.writeLines(file, encoding, lines, append);
        } else {
            FileUtils.writeLines(file, encoding, lines, lineEnding, append);
        }

    }

    /**
     * @param file    目錄/檔案
     * @param isFlile 是否是檔案
     * @return 檔案目錄是否建立成功
     * @throws IOException
     * @方法名：mkdir
     * @方法描述【方法功能描述】建立目錄或建立檔案的父目錄
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 上午11:16:02
     * @修改人：cc
     * @修改時間：2018年8月28日 上午11:16:02
     */
    public static boolean mkdir(File file, boolean isFlile) throws IOException {
        if (isFlile) {
            FileUtils.forceMkdirParent(file);
        } else {
            FileUtils.forceMkdir(file);
        }
        return true;
    }

    /***************************************************************
     * 檔案修改
     */
    /**
     * @param srcDir           起始目錄/起始檔案
     * @param destDir          目的目錄/目的檔案
     * @param preserveFileDate 檔案是否儲存原日期，false儲存最新日期，true儲存元日期
     * @param filter           檔案過濾
     * @throws IOException
     * @方法名：copy
     * @方法描述【方法功能描述】將目錄/檔案複製到目錄/檔案；可過濾；可設定儲存最新日期
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 上午10:15:31
     * @修改人：cc
     * @修改時間：2018年8月28日 上午10:15:31
     */
    public static void copy(File srcDir, File destDir, boolean preserveFileDate, FileFilter filter) throws IOException {
        // 從起始目錄到目的目錄
        if (srcDir.isDirectory() && destDir.isDirectory()) {
            FileUtils.copyDirectory(srcDir, destDir, filter, preserveFileDate);
        }
        // 從起始檔案到目的目錄
        else if (!srcDir.isDirectory() && destDir.isDirectory()) {
            FileUtils.copyFileToDirectory(srcDir, destDir, preserveFileDate);
        }
        // 從起始檔案到目的檔案
        else if (!srcDir.isDirectory() && !destDir.isDirectory()) {
            FileUtils.copyFile(srcDir, destDir, preserveFileDate);
        }

    }

    /**
     * @param src           目錄/檔案
     * @param dest          目錄/檔案
     * @param createDestDir 是否制定建立時間
     * @throws IOException
     * @方法名：move
     * @方法描述【方法功能描述】將目錄/檔案移動到目錄/檔案；可過濾；可設定儲存最新日期
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月29日 上午9:27:48
     * @修改人：cc
     * @修改時間：2018年8月29日 上午9:27:48
     */
    public static void move(File src, File dest, boolean createDestDir) throws IOException {
        // 從起始目錄到目的目錄
        if (src.isDirectory() && dest.isDirectory()) {
            FileUtils.moveDirectoryToDirectory(src, dest, createDestDir);
        }
        // 從起始檔案到目的目錄
        else if (!src.isDirectory() && dest.isDirectory()) {
            FileUtils.moveFileToDirectory(src, dest, createDestDir);
        }
        // 從起始檔案到目的檔案
        else if (!src.isDirectory() && !dest.isDirectory()) {
            FileUtils.moveFile(src, dest);
        }

    }

    /***************************************************************
     * 檔案刪除
     */
    /**
     * @param path 檔案或目錄路徑
     * @throws IOException
     * @方法名：deleteFile
     * @方法描述【方法功能描述】刪除目錄或檔案
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 上午11:04:43
     * @修改人：cc
     * @修改時間：2018年8月28日 上午11:04:43
     */
    public static void deleteFile(String path) throws IOException {
        if (path == null || "".equals(path)) {
            return;
        }
        File ftemp = null;
        try {
            ftemp = new File(path);
            FileUtils.forceDelete(ftemp);
        } catch (IOException e) {
            FileUtils.forceDeleteOnExit(ftemp);
        }
    }

    /**
     * @param file 檔案或目錄物件
     * @throws IOException
     * @方法名：deleteFile
     * @方法描述【方法功能描述】刪除目錄或檔案
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 上午11:05:03
     * @修改人：cc
     * @修改時間：2018年8月28日 上午11:05:03
     */
    public static void deleteFile(File file) throws IOException {
        if (file == null || !file.exists()) {
            return;
        }
        try {
            FileUtils.forceDelete(file);
        } catch (IOException e) {
            FileUtils.forceDeleteOnExit(file);
        }
    }

    /**
     * @param paths 檔案或目錄路徑
     * @throws IOException
     * @方法名：deleteFilesViaPath
     * @方法描述【方法功能描述】 批量刪除檔案或目錄
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 上午11:05:25
     * @修改人：cc
     * @修改時間：2018年8月28日 上午11:05:25
     */
    public static void deleteFilesViaPath(List<String> paths) throws IOException {
        if (paths == null || paths.size() <= 0)
            return;
        for (String path : paths) {
            FileUtil.deleteFile(path);
        }
    }

    /**
     * @param files 檔案或目錄物件集合
     * @throws IOException
     * @方法名：deleteFiles
     * @方法描述【方法功能描述】批量刪除檔案或目錄
     * @修改描述【修改描述】
     * @版本：1.0
     * @建立人：cc
     * @建立時間：2018年8月28日 上午11:05:44
     * @修改人：cc
     * @修改時間：2018年8月28日 上午11:05:44
     */
    public static void deleteFiles(List<File> files) throws IOException {
        if (files == null || files.size() <= 0)
            return;
        for (File file : files) {
            FileUtil.deleteFile(file);
        }
    }

    /**
     * 取得檔案來源於 classpath
     *
     * @param filePath
     * @return
     */
    public static File getFileFromClasspath(String filePath){
        ClassLoader classLoader = FileUtil.class.getClassLoader();
        String replacePath = filePath.replaceFirst("/", "");
        URL resource = classLoader.getResource(replacePath);
        if(Objects.nonNull(resource)){
            return new File(resource.getFile());
        }
        return null;
    }

    public static void main(String[] args) {
        File file1 = new File("d://a/1.txt");
        File file2 = new File("d://b/");
        try {
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}
