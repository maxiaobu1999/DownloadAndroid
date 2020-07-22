package com.malong.download.connect;

public class HttpInfo {
    /** 下载链接 */
    public String download_url;
    /** 保存地址uri */
    public String destination_uri;
    /** 保存地址路径 */
    public String destination_path;
    /** 保存地址路径 */
    public String fileName;

    /** 任务状态： */
//    public int status;
    /** 下载方式 : 重新下载 断点续传 差量下载 分片下载 */
    public int method;
//    /** 下载完成 : 不保存记录 下载中心可见 不可见 */
//    public int complete;
    /** 下载的文件总大小 BIGINT */
    public long total_bytes;
    /** 当前下载的文件大小 BIGINT */
    public long current_bytes;


    /** 分片起点 BIGINT */
    public long start_index;
    /** 分片终点点 BIGINT */
    public long end_index;

//    /** 下载数据的MIME类型 TEXT */
//    public String mime_type;
//
//    /** 分片数量 */
//    public int separate_num;


//    /** 请求头 json */
//    public String header;
}
