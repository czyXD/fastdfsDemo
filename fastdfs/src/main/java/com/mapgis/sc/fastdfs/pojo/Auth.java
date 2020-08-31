package com.mapgis.sc.fastdfs.pojo;

public enum Auth {
    VISTOR(2),AMDIN(1),SUPER_ADMIN(0);
    int number;

    Auth(int number) {
        this.number = number;
    }
}
