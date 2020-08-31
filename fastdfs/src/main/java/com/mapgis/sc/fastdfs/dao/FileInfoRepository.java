package com.mapgis.sc.fastdfs.dao;

import com.mapgis.sc.fastdfs.pojo.FileInformation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileInfoRepository extends JpaRepository<FileInformation,Long> {
    FileInformation findByFileName(String fileName);
    Boolean existsByFileName(String fileName);
    Boolean existsByFileHash(String hash);
}
