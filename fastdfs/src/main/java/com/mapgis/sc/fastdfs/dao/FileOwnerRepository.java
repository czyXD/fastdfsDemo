package com.mapgis.sc.fastdfs.dao;

import com.mapgis.sc.fastdfs.pojo.FileOwner;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FileOwnerRepository extends JpaRepository<FileOwner,Long> {
}
