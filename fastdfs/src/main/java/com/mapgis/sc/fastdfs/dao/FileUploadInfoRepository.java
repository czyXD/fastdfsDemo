package com.mapgis.sc.fastdfs.dao;

import com.mapgis.sc.fastdfs.pojo.FileUploadInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileUploadInfoRepository extends JpaRepository<FileUploadInfo,Integer> {
    FileUploadInfo getByFilenameAndMd5(String fileName,String md5);
}
