package me.stella.service;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;

public class MultithreadedStream {

    public static long[] getSegmentSize(long totalSize, int segments) {
        long[] byteSplit = new long[segments];
        long baseSizePerSegment = totalSize / segments;
        long total = 0;
        for(int i = 0; i < segments - 1; i++) {
            byteSplit[i] = baseSizePerSegment;
            total += baseSizePerSegment;
        }
        byteSplit[segments - 1] = totalSize - total;
        return byteSplit;
    }

    public static CompletableFuture<File> downloadSegment(File directory, String target, long byteStart, long byteEnd) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL downloadURL = new URL(target);
                HttpURLConnection downloadConnection = (HttpURLConnection) downloadURL.openConnection();
                downloadConnection.setRequestProperty("Range", "bytes=" + byteStart + "-" + byteEnd);
                downloadConnection.connect();
                InputStream byteReader = downloadConnection.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(new File(directory, "fs_" + byteStart + "_" + byteEnd));
                byte[] buffer = new byte[65536]; int data;
                while((data = byteReader.read(buffer)) != -1)
                    outputStream.write(buffer, 0, data);
                outputStream.flush(); outputStream.close(); byteReader.close();
                File segment = new File(directory, "fs_" + byteStart + "_" + byteEnd);
                return segment.getAbsoluteFile();
            } catch(Throwable t) { t.printStackTrace(); }
            return null;
        });
    }

    public static CompletableFuture<Boolean> uploadSegment(File file, String target, long byteStart, long byteEnd) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                FileInputStream fileInput = new FileInputStream(file);
                fileInput.skip(byteStart);
                URL uploadURL = new URL(target);
                HttpURLConnection uploadConnection = (HttpURLConnection) uploadURL.openConnection();
                uploadConnection.setRequestMethod("POST");
                uploadConnection.setRequestProperty("Content-Range", "bytes " + byteStart + "-" + byteEnd + "/" + file.length());
                uploadConnection.setRequestProperty("Content-Type", "application/octet-stream");
                uploadConnection.connect();
                OutputStream uploadStream = uploadConnection.getOutputStream();
                byte[] buffer = new byte[65536]; int data;
                long cap = byteEnd - byteStart + 1;
                while((data = fileInput.read(buffer, 0, (int) Math.min(buffer.length, cap))) != -1) {
                    uploadStream.write(buffer, 0, data);
                    cap -= data;
                }
                uploadStream.flush(); uploadStream.close(); fileInput.close();
            } catch(Throwable t) { t.printStackTrace(); }
            return false;
        });
    }

}
