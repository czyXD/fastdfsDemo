package com.mapgis.sc.fastdfs.pojo;

import lombok.Getter;
import lombok.Setter;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.dynamic.loading.InjectionClassLoader;
import org.hibernate.annotations.GeneratorType;
import org.springframework.cglib.core.GeneratorStrategy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;


import javax.persistence.*;
import java.util.Date;


@Entity
@Table
@Getter
@Setter
@EntityListeners(AuditingEntityListener.class)
public class FileInformation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY )
    long id;
    String uploadName;
    String fileName;
    @CreatedBy
    int ownerId;
    String fileHash;
    @CreatedDate
    Date createTime;
    Date deleteTime;
    int status;

    public FileInformation() {
    }

    public FileInformation(String uploadName, int status) {
        this.uploadName = uploadName;
        this.status = status;
    }
}
