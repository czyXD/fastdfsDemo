package com.mapgis.sc.fastdfs.controller;

import com.mapgis.sc.fastdfs.client.FastDFSCliet;
import com.mapgis.sc.fastdfs.client.FastDFSFile;
import com.mapgis.sc.fastdfs.client.ThumbImage;
import com.mapgis.sc.fastdfs.dao.FileDownReposity;
import com.mapgis.sc.fastdfs.dao.FileInfoRepository;
import com.mapgis.sc.fastdfs.dao.FileOwnerRepository;
import com.mapgis.sc.fastdfs.dao.FileUploadInfoRepository;
import com.mapgis.sc.fastdfs.pojo.FileDownloadInfo;
import com.mapgis.sc.fastdfs.pojo.FileInformation;
import com.mapgis.sc.fastdfs.pojo.FileUploadInfo;
import org.csource.common.NameValuePair;
import org.csource.fastdfs.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import javax.transaction.Transactional;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Controller
public class UploadController {
    private static Logger logger = LoggerFactory.getLogger(UploadController.class);
    //充当文件上传唯一性的角色
    private HashMap<String,Integer> fileBuffMap = new HashMap<>();
    final static int DEFAULTSIZE = 10*1024*1024;
    final static int BUFFER_SIZE = 256*1024;
    //大文件保存的地址
    final static String filePath = "/home/download";
    @Autowired
    FileInfoRepository fileInfoRepository;
    @Autowired
    FileOwnerRepository fileOwnerRepository;
    @Autowired
    FileUploadInfoRepository fileUploadInfoRepository;
    @Autowired
    FileDownReposity fileDownReposity;

    /**
     * 根据分组名文件名删除文件
     * @param groupName
     * @param fileName
     */
    @ResponseBody
    @PostMapping("/delete")
    public FileInfo deleteFile(@RequestParam("group_name")String groupName,@RequestParam("file_name") String fileName){
        String name = fileInfoRepository.findByUploadName(fileName).getFileName();
        FileInfo fileInfo = null;
        FileInformation fileInformation = null;
        try {
            fileInfo = FastDFSCliet.getFile(groupName,name);
            fileInformation = fileInfoRepository.findByFileName(name);
            fileInformation.setStatus(1);
            fileInfoRepository.save(fileInformation);

        } catch (Exception e) {
            logger.error("delete file failed",e);
        }
        return fileInfo;
    }

    /**
     * 下载文件,从本地
     * @param fileName
     * @return byte[]
     */
    @GetMapping("/downloadBigFile")
    public void downloadFile(@RequestParam("file_name") String fileName, HttpServletResponse response){
        int pos = 0;
        byte[] buffPart = null;
        File file = new File(filePath + "/" + fileName);
        FileDownloadInfo fileDownloadInfo = new FileDownloadInfo(fileName,filePath + "/" + fileName,null,pos);
        OutputStream outputStream = null;
        InputStream inputStream = null;
        try {
            //设置下载响应头
            response.setHeader("content-disposition", "attachment;filename="+ URLEncoder.encode(fileName, "UTF-8"));
            inputStream = new BufferedInputStream(new FileInputStream(file));
            outputStream = response.getOutputStream();
            buffPart = new byte[BUFFER_SIZE];
            while(inputStream.read(buffPart) != -1){
                outputStream.write(buffPart);
                pos += buffPart.length;
            }
            fileDownloadInfo.setPos(pos);
            fileDownloadInfo.setStatus(0);
            fileDownReposity.save(fileDownloadInfo);
            outputStream.close();
            inputStream.close();
            logger.info("download successed!");
        } catch (UnsupportedEncodingException e) {
            logger.error("code error",e);
        } catch (IOException e) {
            //当输出流发生错误的时候，记录已经下载的文件位置
            fileDownloadInfo.setPos(pos);
            fileDownloadInfo.setStatus(1);
            fileDownReposity.save(fileDownloadInfo);
            logger.error("write file failed!", e);
        }
    }

    /**
     * 流下载文件,从fastdfs
     * @param groupName
     * @param fileUploadName
     * @param response
     * @return
     */
    @GetMapping("/download")
    public int downloadBigFile(@RequestParam("group_name") String groupName, @RequestParam("file_name") String fileUploadName, HttpServletResponse response){
        int pos = 0;
        byte[] buffPart = null;
        //根据上传名获得文件全路径名
        String fileName = fileInfoRepository.findByUploadName(fileUploadName).getFileName();
        long fileSize = FastDFSCliet.getFile(groupName,fileName).getFileSize();
        FileDownloadInfo fileDownloadInfo = null;
        OutputStream outputStream = null;
        try {
            fileDownloadInfo = fileDownReposity.findByUploadNameAndMd5(fileName,null);
            if (fileDownloadInfo == null){
                fileDownloadInfo = new FileDownloadInfo(fileName,fileName,null,0);
            }
            pos = fileDownloadInfo.getPos();
            //设置下载响应头
            response.setHeader("content-disposition", "attachment;filename="+ URLEncoder.encode(fileName, "UTF-8"));
            while (pos < fileSize) {
                int endSize = (int)(fileSize - pos);
                if (endSize > BUFFER_SIZE){
                    buffPart = new byte[BUFFER_SIZE];
                }else {
                    buffPart = new byte[endSize];
                }
                buffPart = FastDFSCliet.downFile(groupName, fileName, pos, buffPart.length);
                outputStream = response.getOutputStream();
                outputStream.write(buffPart);
                if (endSize > BUFFER_SIZE){
                    pos += BUFFER_SIZE;
                }else {
                    pos += endSize;
                }
            }
            fileDownloadInfo.setPos(pos);
            fileDownloadInfo.setStatus(0);
            fileDownReposity.save(fileDownloadInfo);
            outputStream.close();
            logger.info("download successed!");
        } catch (UnsupportedEncodingException e) {
            logger.error("code error",e);
        } catch (IOException e) {
            //当输出流发生错误的时候，记录已经下载的文件位置
            fileDownloadInfo.setPos(pos);
            fileDownloadInfo.setStatus(1);
            fileDownReposity.save(fileDownloadInfo);
            logger.error("write file failed!", e);
            return pos;
        }
        return pos;
    }

    /**
     * 获取文件信息
     * @param groupName
     * @param fileName
     * @return
     */
    @ResponseBody
    @GetMapping("/fileinfo")
    public FileInfo getFileInfo(@RequestParam("group_name") String groupName, @RequestParam("file_name") String fileName){
        String name = fileInfoRepository.findByUploadName(fileName).getFileName();
        FileInfo info =  FastDFSCliet.getFile(groupName,name);
        logger.info("file info:"+info.toString());
        return info;
    }

    /**
     * 通过一个256KB大小的缓冲上传文件
     * @param file
     * @return
     */
    @ResponseBody
    @PostMapping("/uploadByStream")
    public String[] uploadByStream(@RequestParam("file") MultipartFile file) {
        String fileName = file.getOriginalFilename();
        if (null != fileBuffMap.get(fileName)){
            logger.error("file is uploading,chose other one!",new Exception("file is uploading"));
        }
        fileBuffMap.put(fileName,1);
        if (file.isEmpty()) {
            throw new NullPointerException("file is null");
        }
        if (fileInfoRepository.findByUploadName(file.getOriginalFilename()) != null){
            logger.error("file had uplaod!",new Exception("file had uplaod!"));
        }
        String []path = {};
        //缓冲区
        byte[] buff = null;
        //fastdfs保存的组名
        String groupName = "group1";
        //文件后缀
        String ext;
        Long size = file.getSize();
        InputStream inputStream;
        //文件的上传起始位置
        int pos = 0;
        NameValuePair[] meta_list = new NameValuePair[1];
        //上传信息，io流出错时，保存
        FileUploadInfo uploadInfo;
        //文件信息
        FileInformation fileInformation;


        meta_list[0] = new NameValuePair("file_description", file.getResource().getDescription());

        try {
            inputStream = file.getInputStream();
            ext = fileName.substring(fileName.lastIndexOf(".") + 1);
            //保证同步问题
            fileInformation = new FileInformation(fileName,1);
            //从数据库中获取上传信息
            uploadInfo = fileUploadInfoRepository.getByUploadNameAndMd5(fileName,null);
            if (uploadInfo != null){
                pos = uploadInfo.getPos();
            }
            while (pos < size) {
                if (size - pos > BUFFER_SIZE){
                    buff = new byte[BUFFER_SIZE];
                }else {
                    buff = new byte[(int)(size -pos)];
                }
                inputStream.read(buff);
                if (pos == 0) {
                    path = FastDFSCliet.uploadAppenderBystream(groupName,buff,pos,buff.length,ext,meta_list);
                    logger.info("group_name:" + path[0] + "\n" + "file_save_path:" + path[1]);
                    uploadInfo = new FileUploadInfo(fileName,path[1],null,pos);
                    fileUploadInfoRepository.save(uploadInfo);
                }else {
                    int code = FastDFSCliet.appendFile(groupName,path[1],buff,buff.length);
                    if (code == 0){
                        logger.info("append file info success!");
                    }else {
                        //如果追加失败则重新修改文件
                        logger.info("append file error! try modify file");
                        FastDFSCliet.modifyFile(groupName,fileName,pos,buff,buff.length);
                    }
                }
                pos += buff.length;
            }
            fileInformation.setUploadName(fileName);
            fileInformation.setFileName(path[1]);
            fileInformation.setStatus(0);
            fileInfoRepository.save(fileInformation);
            fileBuffMap.replace(fileName,0);
            return path;
        } catch (Exception e) {
            logger.error("upload file failed",e);
            uploadInfo = fileUploadInfoRepository.getByUploadNameAndMd5(fileName,null);
            uploadInfo.setPos(pos);
            uploadInfo.setFilename(path[1]);
            fileUploadInfoRepository.save(uploadInfo);
        }
        return path;
    }

    /**
     * 上传，从fastdfs直接读到内存
     * @param file
     * @return
     */
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

    /**
     * 上传大文件到本地
     * @param file
     * @return
     */
    @ResponseBody
    @PostMapping("/uploadBigFile")
    public String uploadBigFile(@RequestParam("file")MultipartFile file){
        String fileName = file.getOriginalFilename();
        if (null != fileBuffMap.get(fileName)){
            logger.error("file is uploading,chose other one!",new Exception("file is uploading"));
        }
        fileBuffMap.put(fileName,1);
        if (file.isEmpty()) {
            throw new NullPointerException("file is null");
        }
        if (fileInfoRepository.findByUploadName(file.getOriginalFilename()) != null){
            logger.error("file had uplaod!",new Exception("file had uplaod!"));
        }
        //缓冲区
        byte[] buff = null;
        //fastdfs保存的组名
        InputStream inputStream;
        OutputStream outputStream;
        int pos = 0;
        long size = file.getSize();
        FileUploadInfo uploadInfo;
        FileInformation fileInformation;

        try {
            inputStream = file.getInputStream();
            outputStream = new BufferedOutputStream(new FileOutputStream(new File(filePath+"/"+fileName),true));
            //保证同步问题
            fileInformation = new FileInformation(fileName,-1);
            //从数据库中获取上传信息
            uploadInfo = fileUploadInfoRepository.getByUploadName(fileName);
            if (uploadInfo != null){
                pos = uploadInfo.getPos();
            }
            int hasNext = 0;
            while (pos < size && hasNext != -1) {
                if (size - pos > BUFFER_SIZE){
                    buff = new byte[BUFFER_SIZE];
                }else {
                    buff = new byte[(int)(size -pos)];
                }
                hasNext = inputStream.read(buff);
                outputStream.write(buff);
                pos += buff.length;
            }
            fileInformation.setFileName(filePath + "/" + fileName);
            fileInformation.setStatus(0);
            fileInfoRepository.save(fileInformation);
            fileBuffMap.replace(fileName,0);
            return fileInformation.getFileName();
        } catch (Exception e) {
            logger.error("upload file failed",e);
            uploadInfo = new FileUploadInfo(fileName,filePath + "/" + fileName,null,pos);
            fileUploadInfoRepository.save(uploadInfo);
        }
        return null;
    }

    /**
     * 大文件根据md5上传
     * @param file
     * @param md5
     * @return
     */
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
                uploadInfo = fileUploadInfoRepository.getByUploadNameAndMd5(tempFileInfo.getName(), tempFileInfo.getMd5());
                pos = uploadInfo.getPos();
            }
            tempFileInfo.setPos(pos);
            logger.info(tempFileInfo.toString());
            if (pos == 0) {
                path = FastDFSCliet.uploadAppender(tempFileInfo);
                //将第一次上传的信息存入数据库
                uploadInfo.setUploadName(tempFileInfo.getName());
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
                fileInformation.setUploadName(tempFileInfo.getName());
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
            return "group_name:"+path[0] + " file_remote_name:" + path[1] + " file_true_name:" + fileInformation.getUploadName();
            } catch (IOException ioException) {
            ioException.printStackTrace();
        }

        return null;
    }

    /**
     * 分块测试
     * @param file
     * @return
     */
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
            logger.info(fileAbsolutePath[1]);
            //储存到数据库中
            fileInformation.setUploadName(fileName);
            fileInformation.setFileName(fileAbsolutePath[1]);
            fileInformation.setStatus(0);
            fileInformation.setFileHash(md5);
            fileInfoRepository.save(fileInformation);
        } catch (Exception e) {
            logger.error("upload file Exception!",e);
        }
        if (fileAbsolutePath==null) {
            logger.error("upload file failed,please upload again!");
        }
        return fileAbsolutePath;
    }

    /**
     * 生成缩略图
     * @param file
     * @param isThumnImage
     * @throws IOException
     */
    public void generateThum(FastDFSFile file,boolean isThumnImage) throws IOException {
        byte[] thumByte = null;
        String thumAbsolutePath[] = null;
        if (isThumnImage) {
            ThumbImage thumbImage = new ThumbImage(150, 150);
            thumByte = thumbImage.getThumbImage(new ByteArrayInputStream(file.getContent())).toByteArray();
        }
        thumAbsolutePath = FastDFSCliet.upload(new FastDFSFile(file.getName()+"150*150",thumByte,"jpg"));
        logger.info(thumAbsolutePath[0] + thumAbsolutePath[1]);

    }
}
