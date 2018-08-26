package com.my.blog.website.utils;

import com.my.blog.website.utils.qiniu.QiniuCloudUtil;
import com.qiniu.storage.BucketManager;
import com.qiniu.storage.model.FileInfo;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * zip压缩工具类
 */
public class ZipUtils {

    public static void zipFolder(String srcFolder, String destZipFile) throws Exception {
        ZipOutputStream zip = null;
        FileOutputStream fileWriter = null;

        fileWriter = new FileOutputStream(destZipFile);
        zip = new ZipOutputStream(fileWriter);

        //addFolderToZip("", srcFolder, zip);
        //获取七牛云空间文件列表
        QiniuCloudUtil qiniuCloudUtil = new QiniuCloudUtil();
        BucketManager.FileListIterator fileList = qiniuCloudUtil.getFileList();

        FileInfo[] items = fileList.next();
        System.out.println(items.length);
        for (FileInfo item : items) {
            String srcPath = srcFolder+"/"+item.key;
            addFileToZip1("",srcPath,zip);
        }

        zip.flush();
        zip.close();
    }

    public static void zipFile(String filePath, String zipPath) throws Exception{
        byte[] buffer = new byte[1024];
        FileOutputStream fos = new FileOutputStream(zipPath);
        ZipOutputStream zos = new ZipOutputStream(fos);
        ZipEntry ze= new ZipEntry("spy.log");
        zos.putNextEntry(ze);
        FileInputStream in = new FileInputStream(filePath);
        int len;
        while ((len = in.read(buffer)) > 0) {
            zos.write(buffer, 0, len);
        }
        in.close();
        zos.closeEntry();
        //remember close it
        zos.close();
    }

    public static void addFileToZip(String path, String srcFile, ZipOutputStream zip)
            throws Exception {

        File folder = new File(srcFile);
        if (folder.isDirectory()) {
            addFolderToZip(path, srcFile, zip);
        } else {
            byte[] buf = new byte[1024];
            int len;
            FileInputStream in = new FileInputStream(srcFile);
            zip.putNextEntry(new ZipEntry(path + "/" + folder.getName()));
            while ((len = in.read(buf)) > 0) {
                zip.write(buf, 0, len);
            }
        }
    }

    public static void addFileToZip1(String path, String srcFile, ZipOutputStream zip)
            throws Exception {

        try {
            URL url = new URL(srcFile); // 创建URL
            URLConnection urlconn = url.openConnection(); // 试图连接并取得返回状态码
            urlconn.connect();
            HttpURLConnection httpconn = (HttpURLConnection) urlconn;
            int responseCode = httpconn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                System.out.print("无法连接到");
            } else {
                int filesize = urlconn.getContentLength(); // 取数据长度
                InputStreamReader isReader = new InputStreamReader(urlconn.getInputStream(), "UTF-8");
                InputStream in = urlconn.getInputStream();

                byte[] buf = new byte[1024];
                int len;
                String filename = srcFile.substring(srcFile.lastIndexOf("/")+1);
                if(!filename.endsWith(".jpg")){
                    filename+=".jpg";
                }

                zip.putNextEntry(new ZipEntry(path + "/"+filename));
                while ((len = in.read(buf)) > 0) {
                    zip.write(buf, 0, len);
                }

            }
        }catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void addFolderToZip(String path, String srcFolder, ZipOutputStream zip) throws Exception {
        File folder = new File(srcFolder);
        if (null != path && folder.isDirectory()) {
            String[] fileList = folder.list();
            if (fileList != null) {
                for (String fileName : fileList) {
                    if (path.equals("")) {
                        addFileToZip(folder.getName(), srcFolder + "/" + fileName, zip);
                    } else {
                        addFileToZip(path + "/" + folder.getName(), srcFolder + "/" + fileName, zip);
                    }
                }
            }
        }
    }

} 