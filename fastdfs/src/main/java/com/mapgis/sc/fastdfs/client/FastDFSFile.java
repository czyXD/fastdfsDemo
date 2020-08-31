package com.mapgis.sc.fastdfs.client;

/**
 * 储存文件数据结构
 */
public class FastDFSFile {
    @Override
    public String toString() {
        return "FastDFSFile{" +
                "name='" + name + '\'' +
                ", ext='" + ext + '\'' +
                ", md5='" + md5 + '\'' +
                ", author='" + author + '\'' +
                ", size=" + size +
                '}';
    }

    private String name;

    private byte[] content;

    private String ext;

    private String md5;

    private String author;

    private long size;

    //上一次上传的位置
    private long pos;


    private boolean uploadThumblamge;

    public boolean isUploadThumblamge() {
        return uploadThumblamge;
    }

    public void setUploadThumblamge(boolean uploadThumblamge) {
        this.uploadThumblamge = uploadThumblamge;
    }
    public long getPos() {
        return pos;
    }

    public void setPos(long pos) {
        this.pos = pos;
    }

    public long getSize() {
        return size;
    }
    public FastDFSFile() {
    }

    public FastDFSFile(String name, String md5) {
        this.name = name;
        this.md5 = md5;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public FastDFSFile(String name, byte[] content, String ext, String height,
                       String width, String author) {
        super();
        this.name = name;
        this.content = content;
        this.ext = ext;
        this.author = author;
    }

    public FastDFSFile(String name, byte[] content, String ext) {
        super();
        this.name = name;
        this.content = content;
        this.ext = ext;

    }
    public FastDFSFile(String name, byte[] content) {
        super();
        this.name = name;
        this.content = content;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }
}
