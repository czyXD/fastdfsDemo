package com.mapgis.sc.fastdfs.dao;

import com.mapgis.sc.fastdfs.pojo.FileDownloadInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileDownReposity extends JpaRepository<FileDownloadInfo,Long> {
    FileDownloadInfo findByUploadNameAndMd5(String uploadName,String md5);
}
