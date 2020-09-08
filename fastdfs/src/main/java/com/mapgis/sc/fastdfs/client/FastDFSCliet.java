package com.mapgis.sc.fastdfs.client;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.csource.common.MyException;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FastDFSCliet {
    final static int DEFAULTSIZE = 500*1024*1024;
    private static Logger logger = LoggerFactory.getLogger(FastDFSCliet.class);
    //导入配置文件
    static{
        try {
            ClassPathResource classPathResource = new ClassPathResource("fdfs_client.conf");
            //创建临时文件，将fdfs_client.conf的值赋值到临时文件中，创建这个临时文件的原因是springboot打成jar后无法获取classpath下文件
            String tempPath =System.getProperty("java.io.tmpdir") + System.currentTimeMillis()+".conf";
            File f = new File(tempPath);
            IOUtils.copy(classPathResource.getInputStream(),new FileOutputStream(f));
            ClientGlobal.init(f.getAbsolutePath());
            System.out.println("ClientGlobal.configInfo(): " + ClientGlobal.configInfo());
        } catch (Exception e) {
            logger.error("FastDFS Client Init Fail!",e);
        }
    }
    public static String[] uploadAppenderBystream(String groupName,byte[] buff,int pos,int size,String ext,NameValuePair[] nameValuePair){
        String[] uploadReults;
        StorageClient storageClient;
        try {
            storageClient = getTrackerClient();
            uploadReults = storageClient.upload_appender_file(groupName,buff,pos,size,ext,nameValuePair);
            return uploadReults;
        } catch (IOException e) {
            logger.error("get storageclien fail",e);
        } catch (MyException e) {
            logger.error("upload append file fail",e);
        }
        return null;
    }
    public static String[] uploadAppender(FastDFSFile file){

        NameValuePair[] meta_list = new NameValuePair[1];
        meta_list[0] = new NameValuePair("author", file.getAuthor());

        //记录上传起始时间戳
        long startTime = System.currentTimeMillis();
        String[] uploadResults = null;
        StorageClient storageClient=null;
        try {
            storageClient = getTrackerClient();
            //上传文件
            uploadResults = storageClient.upload_appender_file(file.getContent(), file.getExt(), meta_list);
        } catch (IOException e) {
            logger.error("IO Exception when uploadind the file:" + file.getName(), e);
        } catch (Exception e) {
            logger.error("Non IO Exception when uploadind the file:" + file.getName(), e);
        }
        logger.info("upload_file time used:" + (System.currentTimeMillis() - startTime) + " ms");
        if (uploadResults == null && storageClient!=null) {
            logger.error("upload file fail, error code:" + storageClient.getErrorCode());
        }
        String groupName = uploadResults[0];
        String remoteFileName = uploadResults[1];

        logger.info("upload file successfully!!!" + "group_name:" + groupName + ", remoteFileName:" + " " + remoteFileName);
        return uploadResults;
    }
    public static int uploadChunks(FastDFSFile file){
        long startTime = System.currentTimeMillis();
        StorageClient storageClient = null;
        int resultCode = 0;
        try {
            storageClient = getTrackerClient();
            byte[] appendFile = file.getContent();
//            byte[] buffer = new byte[appendFile.length];
            //以修改的方式解决这一问题，追加可能会出现重复追加的情况
            storageClient.append_file("group1",file.getName(),appendFile,0,appendFile.length);
//            resultCode = storageClient.modify_file("group0",file.getName(),file.getPos(),appendFile,0,appendFile.length);
            long endtime = System.currentTimeMillis() - startTime;
            if (resultCode == 0 ){
                logger.info("upload chunk file success!:"+ file.getName() + "time:" + endtime);
            }
        } catch (IOException e) {
            logger.error("IO Exception when uploading the chunkfile:" + file.getName(),e);
        } catch (MyException e) {
            logger.error("Exception when upload then chunkfile by modify:" + file.getName(),e);
        }


        return resultCode;
    }
    public static void modifyFile(String groupName,String fileName,int pos,byte[] appendfile,int size){
        StorageClient storageClient = null;
        int resultCode = 0;
        try {
            storageClient = getTrackerClient();
            resultCode = storageClient.modify_file("group1",fileName,pos,appendfile,0,size);
            logger.info("modify file success!:"+ fileName );
        } catch (IOException e) {
            logger.error("IO Exception when uploading the chunkfile:" + fileName,e);
        } catch (MyException e) {
            logger.error("Exception when upload then chunkfile by modify:" + fileName,e);
        }
    }
    public static int appendFile(String groupName,String fileName,byte[] appendfile,int size){
        long startTime = System.currentTimeMillis();
        StorageClient storageClient = null;
        int resultCode = 0;
        try {
            storageClient = getTrackerClient();
            resultCode = storageClient.append_file("group1",fileName,appendfile,0,size);
            long endtime = System.currentTimeMillis() - startTime;
            logger.info("upload chunk file success!:"+ fileName + "time:" + endtime);
            return resultCode;
        } catch (IOException e) {
            logger.error("IO Exception when uploading the chunkfile:" + fileName,e);
        } catch (MyException e) {
            logger.error("Exception when upload then chunkfile by modify:" + fileName,e);
        }


        return 0;
    }
    public static String[] upload(FastDFSFile file){
        logger.info("File Name: " + file.getName() + "File Length:" + file.getContent().length);

        NameValuePair[] meta_list = new NameValuePair[1];
        meta_list[0] = new NameValuePair("author", file.getAuthor());
        //记录上传起始时间戳
        long startTime = System.currentTimeMillis();
        String[] uploadResults = null;
        StorageClient storageClient=null;
        try {
            storageClient = getTrackerClient();
            //上传文件
            uploadResults = storageClient.upload_file(file.getContent(), file.getExt(), meta_list);
        } catch (IOException e) {
            logger.error("IO Exception when uploadind the file:" + file.getName(), e);
        } catch (Exception e) {
            logger.error("Non IO Exception when uploadind the file:" + file.getName(), e);
        }
        logger.info("upload_file time used:" + (System.currentTimeMillis() - startTime) + " ms");

        if (uploadResults == null && storageClient!=null) {
            logger.error("upload file fail, error code:" + storageClient.getErrorCode());
        }
        String groupName = uploadResults[0];
        String remoteFileName = uploadResults[1];

        logger.info("upload file successfully!!!" + "group_name:" + groupName + ", remoteFileName:" + " " + remoteFileName);
        return uploadResults;
    }
    public static FileInfo getFile(String groupName, String remoteFileName) {
        try {
            StorageClient storageClient = getTrackerClient();
            return storageClient.get_file_info(groupName, remoteFileName);
        } catch (IOException e) {
            logger.error("IO Exception: Get File from Fast DFS failed", e);
        } catch (Exception e) {
            logger.error("Non IO Exception: Get File from Fast DFS failed", e);
        }
        return null;
    }

    public static byte[] downFile(String groupName, String remoteFileName) {
        try {
            StorageClient storageClient = getTrackerClient();
            return storageClient.download_file(groupName, remoteFileName);
        } catch (IOException e) {
            logger.error("IO Exception: Get File from Fast DFS failed", e);
        } catch (Exception e) {
            logger.error("Non IO Exception: Get File from Fast DFS failed", e);
        }
        return null;
    }
    public static byte[] downFile(String groupName,String remoteFileName,int offset,long byteSize){
        try {
            StorageClient storageClient = getTrackerClient();
            return storageClient.download_file(groupName, remoteFileName,offset,byteSize);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (MyException e) {
            e.printStackTrace();
        }
        return null;
    }
    public static FileInfo deleteFile(String groupName, String remoteFileName)
            throws Exception {
        StorageClient storageClient = getTrackerClient();
        FileInfo fileInfo = storageClient.get_file_info(groupName, remoteFileName);
        if (null == fileInfo){
            logger.error("file has been delete or someone used");
            throw new NullPointerException("file is null");
        }
        int i = storageClient.delete_file(groupName, remoteFileName);
        logger.info("delete file successfully!!!" + i);
        return fileInfo;
    }

    public static StorageServer[] getStoreStorages(String groupName)
            throws IOException, MyException {
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = trackerClient.getTrackerServer();
        return trackerClient.getStoreStorages(trackerServer, groupName);
    }

    public static ServerInfo[] getFetchStorages(String groupName,
                                                String remoteFileName) throws IOException, MyException {
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = trackerClient.getTrackerServer();
        return trackerClient.getFetchStorages(trackerServer, groupName, remoteFileName);
    }

    public static String getTrackerUrl() throws IOException {
        return "http://"+getTrackerServer().getInetSocketAddress().getHostString()+":"+ClientGlobal.getG_tracker_http_port()+"/";
    }

    private static StorageClient getTrackerClient() throws IOException {
        TrackerServer trackerServer = getTrackerServer();
        StorageClient storageClient = new StorageClient(trackerServer, null);
        return  storageClient;
    }

    private static TrackerServer getTrackerServer() throws IOException {
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer trackerServer = trackerClient.getTrackerServer();
        return  trackerServer;
    }
}
