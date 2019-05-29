package com.company;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

import org.apache.commons.collections.map.MultiValueMap;


public class Main {
    public static String REPORTING_DATE_FORMAT;
    public static String REPORTING_DATE_STR;

    static {
        REPORTING_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss z";
    }

    public static boolean uploadSingleFile(FTPClient ftpClient,
                                           String localFilePath, String remoteFilePath) throws IOException {
        File localFile = new File(localFilePath);

        InputStream inputStream = new FileInputStream(localFile);
        try {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            return ftpClient.storeFile(remoteFilePath, inputStream);
        } finally {
            inputStream.close();
        }
    }

    static void uploadDirectory(FTPClient ftpClient,
                                String remoteDirPath, String localParentDir, String remoteParentDir) throws IOException {
        System.out.println("LISTING directory: " + localParentDir);

        File localDir = new File(localParentDir);
        File[] subFiles = localDir.listFiles();
        if (subFiles != null && subFiles.length > 0) {
            for (File item : subFiles) {
                String remoteFilePath = remoteDirPath + "/" + remoteParentDir
                        + "/" + item.getName();
                if (remoteParentDir.equals("")) {
                    remoteFilePath = remoteDirPath + "/" + item.getName();
                }

                if (item.isFile()) {
                    // upload the file
                    String localFilePath = item.getAbsolutePath();
                    System.out.println("About to upload the file: " + localFilePath);
                    boolean uploaded = uploadSingleFile(ftpClient,
                            localFilePath, remoteFilePath);
                    if (uploaded) {
                        System.out.println("UPLOADED a file to: "
                                + remoteFilePath);
                    } else {
                        System.out.println("COULD NOT upload the file: "
                                + localFilePath);
                    }
                } else {
                    // create directory on the server
                    boolean created = ftpClient.makeDirectory(remoteFilePath);
                    if (created) {
                        System.out.println("CREATED the directory: "
                                + remoteFilePath);
                    } else {
                        System.out.println("COULD NOT create the directory: "
                                + remoteFilePath + " Directory exists!");
                    }

                    // upload the sub directory
                    String parent = remoteParentDir + "/" + item.getName();
                    if (remoteParentDir.equals("")) {
                        parent = item.getName();
                    }

                    localParentDir = item.getAbsolutePath();
                    uploadDirectory(ftpClient, remoteDirPath, localParentDir,
                            parent);
                }
            }
        }
    }

    static void uploadDirectory(String host, int port, String user, String password,
                                String remoteDirPath, String localParentDir, String remoteParentDir)
            throws IOException {
        FTPClient ftpClient = new FTPClient();
        System.out.println("Connecting to [" + host + ":" + port + "]");
        ftpClient.connect(host, port);
        if (FTPReply.isPositiveCompletion(ftpClient.getReplyCode())) {
            System.out.println("Logging in as [" + user + "]");
        }
        if (ftpClient.login(user, password)) {
            uploadDirectory(ftpClient, remoteDirPath, localParentDir, remoteParentDir);
        }
    }

    public static void main(String[] args) throws IOException {

        MultiValueMap map = new MultiValueMap();
        map.put("alexv", "123");
        map.put("alexv", "222");
        map.put("alexv", "1234");
        map.put("alexv", "1235");
        map.put("andreypo", "1235");

        for (Object o : map.entrySet())
        {
            Map.Entry entry = (Map.Entry) o;
            List values = (List) map.getCollection(entry.getKey());
            Collections.sort(values);
            System.out.println(entry.getKey() + " " + values);
        }

        System.out.println(new SimpleDateFormat("yyyy/MM/dd HH:mm").format(new Date()));
    }

    public static List<File> getFiles(String location) {

        File folder = new File(location);
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains(REPORTING_DATE_STR);
            }
        };
        File[] listOfFiles = folder.listFiles(filter);
        return Arrays.asList(listOfFiles);
    }
}
