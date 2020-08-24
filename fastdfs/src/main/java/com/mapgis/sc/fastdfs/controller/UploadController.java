package com.mapgis.sc.fastdfs.controller;

import com.mapgis.sc.fastdfs.client.FastDFSCliet;
import com.mapgis.sc.fastdfs.client.FastDFSFile;
import org.csource.fastdfs.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.*;

@Controller
public class UploadController {
    private static Logger logger = LoggerFactory.getLogger(UploadController.class);

    /**
     * 根据分组名文件名删除文件
     * @param groupName
     * @param fileName
     */
    @DeleteMapping("/delete")
    public void deleteFile(@RequestParam("group_name")String groupName,@RequestParam("file_name") String fileName){
        try {
            FastDFSCliet.deleteFile(groupName,fileName);
        } catch (Exception e) {
            logger.error("delete file failed",e);
        }
    }

    /**
     * 下载文件
     * @param groupName
     * @param fileName
     * @return byte[]
     */
    @GetMapping("/download")
    public byte[] downloadFile(@RequestParam("group_name") String groupName, @RequestParam("file_name") String fileName){
        return FastDFSCliet.downFile(groupName,fileName);
    }
    @ResponseBody
    @GetMapping("/fileinfo")
    public FileInfo getFileInfo(@RequestParam("group_name") String groupName, @RequestParam("file_name") String fileName){
        return FastDFSCliet.getFile(groupName,fileName);
    }

    @PostMapping("/upload")
    public String singleFileUpload(@RequestParam("file") MultipartFile file,
                                   RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("message", "Please select a file to upload");
            return "redirect:uploadStatus";
        }
        try {
            String path=saveFile(file);
            redirectAttributes.addFlashAttribute("message",
                    "You successfully uploaded '" + file.getOriginalFilename() + "'");
            redirectAttributes.addFlashAttribute("path",
                    "file path url '" + path + "'");
        } catch (Exception e) {
            logger.error("upload file failed",e);
        }
        return "redirect:/uploadStatus";
    }

    @GetMapping("/uploadStatus")
    public String uploadStatus() {
        return "uploadStatus";
    }

    /**
     * @param multipartFile
     * @return
     * @throws IOException
     */
    public String saveFile(MultipartFile multipartFile) throws IOException {
        String[] fileAbsolutePath={};
        String fileName=multipartFile.getOriginalFilename();
        String ext = fileName.substring(fileName.lastIndexOf(".") + 1);
        byte[] file_buff = null;
        InputStream inputStream=multipartFile.getInputStream();
        if(inputStream!=null){
            int len1 = inputStream.available();
            file_buff = new byte[len1];
            inputStream.read(file_buff);
        }
        inputStream.close();
        FastDFSFile file = new FastDFSFile(fileName, file_buff, ext);
        try {
            fileAbsolutePath = FastDFSCliet.upload(file);
        } catch (Exception e) {
            logger.error("upload file Exception!",e);
        }
        if (fileAbsolutePath==null) {
            logger.error("upload file failed,please upload again!");
        }
        String path=FastDFSCliet.getTrackerUrl()+fileAbsolutePath[0]+ "/"+fileAbsolutePath[1];
        return path;
    }
}
