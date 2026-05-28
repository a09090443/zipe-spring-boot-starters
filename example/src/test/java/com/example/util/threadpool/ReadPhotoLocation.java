package com.example.util.threadpool;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.GpsDirectory;

import java.io.File;

/**
 * @author zipe1
 * @created 2025/7/8
 */
public class ReadPhotoLocation {
    public static void main(String[] args) throws Exception {
        File jpegFile = new File("D:/tmp/PXL_20250701_104130643.jpg");
        Metadata metadata = ImageMetadataReader.readMetadata(jpegFile);

        for (Directory directory : metadata.getDirectories()) {
            if (directory instanceof GpsDirectory) {
                GpsDirectory gpsDir = (GpsDirectory) directory;
                // 取得經緯度
                com.drew.lang.GeoLocation geoLocation = gpsDir.getGeoLocation();
                if (geoLocation != null) {
                    System.out.println("Latitude: " + geoLocation.getLatitude());
                    System.out.println("Longitude: " + geoLocation.getLongitude());
                } else {
                    System.out.println("此照片無 GPS 資訊");
                }
            }
        }
    }
}
