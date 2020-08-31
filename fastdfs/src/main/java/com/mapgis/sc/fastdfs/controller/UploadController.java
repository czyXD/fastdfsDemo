package com.mapgis.sc.fastdfs.controller;

import com.mapgis.sc.fastdfs.client.FastDFSCliet;
import com.mapgis.sc.fastdfs.client.FastDFSFile;
import com.mapgis.sc.fastdfs.client.ThumbImage;
import com.mapgis.sc.fastdfs.dao.FileInfoRepository;
import com.mapgis.sc.fastdfs.dao.FileOwnerRepository;
import com.mapgis.sc.fastdfs.dao.FileUploadInfoRepository;
import com.mapgis.sc.fastdfs.pojo.FileInformation;
import com.mapgis.sc.fastdfs.pojo.FileUploadInfo;
import org.csource.fastdfs.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Controller
public class UploadController {
    private static Logger logger = LoggerFactory.getLogger(UploadController.class);
    final static int DEFAULTSIZE = 10*1024*1024;

    @Autowired
    FileInfoRepository fileInfoRepository;
    @Autowired
    FileOwnerRepository fileOwnerRepository;
    @Autowired
    FileUploadInfoRepository fileUploadInfoRepository;

    /**
     * 根据分组名文件名删除文件
     * @param groupName
     * @param fileName
     */
    @ResponseBody
    @PostMapping("/delete")
    public FileInfo deleteFile(@RequestParam("group_name")String groupName,@RequestParam("file_name") String fileName){
        FileInfo fileInfo = null;
        FileInformation fileInformation = null;
        try {
            fileInfo = FastDFSCliet.getFile(groupName,fileName);
            fileInformation = fileInfoRepository.findByFileName(fileName);
            fileInformation.setStatus(1);
            fileInfoRepository.save(fileInformation);

        } catch (Exception e) {
            logger.error("delete file failed",e);
        }
        return fileInfo;
    }

    /**
     * 下载文件
     * @param groupName
     * @param fileName
     * @return byte[]
     */
    @GetMapping("/download")
    public void downloadFile(@RequestParam("group_name") String groupName, @RequestParam("file_name") String fileName, HttpServletResponse response){
        byte[] temp = FastDFSCliet.downFile(groupName,fileName);
        FileOutputStream outputStream;
        if (null == temp){
            logger.error("file is null,choose other one");
        }
        try {
            //设置响应头
            response.setHeader("content-disposition", "attachment;filename="+ URLEncoder.encode(fileName, "UTF-8"));
            response.getOutputStream().write(temp);
        } catch (IOException e) {
            logger.error("download faild",e);
        }
    }
    @ResponseBody
    @GetMapping("/fileinfo")
    public FileInfo getFileInfo(@RequestParam("group_name") String groupName, @RequestParam("file_name") String fileName){
        FileInfo info =  FastDFSCliet.getFile(groupName,fileName);
        logger.info("file info:"+info.toString());
        return info;
    }

    @ResponseBody
    @PostMapping("/upload")
    public String[] singleFileUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            throw new NullPointerException("file is null");
        }
        String []path = {};
        try {
            path = saveFile(file);

        } catch (Exception e) {
            logger.error("upload file failed",e);
        }
        return path;
    }
    @PostMapping("/chunksUpload")
    public String chunksUpload(MultipartFile file,String md5) {
        //返回数据
        String path[] = null;
        try {
            InputStream inputStream = file.getInputStream();
            //缓冲字节数组
            byte[] bufferAppender = null;
            //存到数据库中的数据
            FileInformation fileInformation = new FileInformation();
            FileUploadInfo uploadInfo = new FileUploadInfo();
            //文件信息
            FastDFSFile tempFileInfo = new FastDFSFile(file.getOriginalFilename(),md5);
            tempFileInfo.setExt(tempFileInfo.getName().substring(tempFileInfo.getName().lastIndexOf(".")+1));
            //起始位置
            int pos = 0;
            int fileSize = (int) file.getSize();
            if (DEFAULTSIZE >= file.getSize()){
                bufferAppender = new byte[fileSize];
            }else{
                logger.error("size of file is out bound!",new ArrayIndexOutOfBoundsException("file is too big!!!"));
            }
            int readSize = inputStream.read(bufferAppender);
            inputStream.close();
            //判断文件是否丢失
            if (readSize != fileSize){
                logger.error("some file has been lost",new Exception());
            }
            tempFileInfo.setContent(bufferAppender);
            tempFileInfo.setSize(fileSize);
            //从临时中获取上传的位置信息
            if (pos != 0) {
                uploadInfo = fileUploadInfoRepository.getByFilenameAndMd5(tempFileInfo.getName(), tempFileInfo.getMd5());
                pos = uploadInfo.getPos();
            }
            tempFileInfo.setPos(pos);
            logger.info(tempFileInfo.toString());
            if (pos == 0) {
                path = FastDFSCliet.uploadAppender(tempFileInfo);
                //将第一次上传的信息存入数据库
                uploadInfo.setFilename(path[1]);
                uploadInfo.setMd5(md5);
                uploadInfo.setPos(pos + fileSize);
                fileUploadInfoRepository.save(uploadInfo);
                //将文件的完整信息存入数据库，状态为2，表示上传未完成
                if (fileSize < DEFAULTSIZE){
                    fileInformation.setStatus(0);
                }else {
                    fileInformation.setStatus(2);
                }
                fileInformation.setFileName(path[1]);
                fileInformation.setFileHash(md5);
                fileInfoRepository.save(fileInformation);
            }else {
                int errorCode = FastDFSCliet.uploadChunks(tempFileInfo);
                if (errorCode != 0){
                    logger.error("upload file failed!",new Exception("upload chunk file failed"));
                }
                uploadInfo.setPos(pos + fileSize);
                fileUploadInfoRepository.save(uploadInfo);
            }
            return "group_name:"+path[0] + " file_remote_name:" + path[1];
            } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return null;
    }
    @ResponseBody
    @PostMapping("/chunktest")
    public String test(MultipartFile file) {
        try {
            InputStream inputStream = file.getInputStream();
            List<byte[]> files = new ArrayList<>();
            FileInformation fileInformation = new FileInformation();
            int pos = 0;
            byte[]appendFile = new byte[inputStream.available()];
            inputStream.read(appendFile);
            inputStream.close();
            logger.info("read is success");
            long fileSize = appendFile.length;
            //对文件分块
            for (int i = 0;i <= fileSize/DEFAULTSIZE;i ++){
                byte[] temp;
                if (fileSize - i * DEFAULTSIZE > DEFAULTSIZE ) {
                    temp = new byte[DEFAULTSIZE];
                }else {
                    temp = new byte[(int) (fileSize - i * DEFAULTSIZE)];
                }
                System.arraycopy(appendFile,pos,temp,0,temp.length);
                pos += temp.length;
                files.add(temp);
            }
            logger.info("分块成功");
            pos = 0;
            String []path = null;
            FastDFSFile buffer = new FastDFSFile(file.getOriginalFilename(),DigestUtils.md5DigestAsHex(appendFile));
            String ext = buffer.getName().substring(buffer.getName().lastIndexOf(".")+1);
            buffer.setExt(ext);
            for (int i = 0;i < files.size();i ++){
                byte[] temp = files.get(i);
                buffer.setContent(temp);
                buffer.setSize(temp.length);
                buffer.setPos(pos);
                logger.info(buffer.toString());
                //第一次上传文件，接受返回的路径
                if (i == 0) {
                    path=FastDFSCliet.uploadAppender(buffer);
                    buffer.setName(path[1]);
                    logger.info("first upload file success:"+path[1]);
                }
                else {
                    FastDFSCliet.uploadChunks(buffer);
                }
                pos += temp.length;
            }
            //存到数据库
            fileInformation.setFileName(buffer.getName());
            fileInformation.setStatus(1);
            fileInformation.setFileHash(buffer.getMd5());
            fileInfoRepository.save(fileInformation);
            return "group_name:"+path[0] + "file_remote_name:" + path[1] + "file_md5:" + buffer.getMd5();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //第一次上传文件的路径
        return null;
    }


    /**
     * @param multipartFile
     * @return
     * @throws IOException
     */
    public String[] saveFile(MultipartFile multipartFile) throws Exception {
        String[] fileAbsolutePath= {};
        String fileName=multipartFile.getOriginalFilename();
        FileInformation fileInformation = new FileInformation();
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
        byte[] file_buff = null;
        int size = 0;
        if (fileInfoRepository.existsByFileName(fileName)){
            logger.error("file has exist");
            throw new Exception("file has exist");
        }
        InputStream inputStream=multipartFile.getInputStream();
        if(inputStream != null){
            int len1 = inputStream.available();
            file_buff = new byte[len1];
            size = inputStream.read(file_buff);
        }
        inputStream.close();
        //计算文件的md5值
        String md5 = DigestUtils.md5DigestAsHex(file_buff);

        FastDFSFile file = new FastDFSFile(fileName, file_buff, ext);
        file.setMd5(md5);
        file.setSize(file_buff.length);
        try {
            fileAbsolutePath = FastDFSCliet.upload(file);
            boolean isThumnImage = true;
            byte[] thumByte = null;
            String thumAbsolutePath[] = null;
            //生成缩略图
            if (isThumnImage) {
                ThumbImage thumbImage = new ThumbImage(150, 150);
                thumByte = thumbImage.getThumbImage(new ByteArrayInputStream(file_buff)).toByteArray();
            }
            thumAbsolutePath = FastDFSCliet.upload(new FastDFSFile(fileName+"150*150",thumByte,"jpg"));
            logger.info(fileAbsolutePath[1]);
            //储存到数据库中
            fileInformation.setFileName(fileAbsolutePath[1]);
            fileInformation.setStatus(0);
            fileInformation.setFileHash(md5);
            fileInfoRepository.save(fileInformation);
            logger.info(thumAbsolutePath[0] + thumAbsolutePath[1]);
        } catch (Exception e) {
            logger.error("upload file Exception!",e);
        }
        if (fileAbsolutePath==null) {
            logger.error("upload file failed,please upload again!");
        }
        return fileAbsolutePath;
    }
}
