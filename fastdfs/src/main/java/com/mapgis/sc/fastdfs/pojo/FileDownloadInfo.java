package com.mapgis.sc.fastdfs.pojo;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Table
@Getter
@Setter
public class FileDownloadInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    int id;
    String user;
    String uploadName;
    String filename;
    String md5;
    int pos;
    int status;

    public FileDownloadInfo() {
    }

    public FileDownloadInfo(String uploadName, String filename, String md5, int pos) {
        this.filename = filename;
        this.md5 = md5;
        this.pos = pos;
    }
}
