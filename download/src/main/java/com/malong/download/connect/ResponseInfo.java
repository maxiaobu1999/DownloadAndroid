package com.malong.download.connect;

public class ResponseInfo {
//    Response Header：
    // 表示该服务器支持范围请求
//    Accept-Ranges : bytes
//    Server : nginx
//    ETag : "4df4d61142e773a16769473cf2654b71"
//    x-cos-request-id : NWYxNjU5ZmFfOGViMjM1MGFfNTNkZV8zMGJlMWY=
//    Connection : close
//    Last-Modified : Sun, 12 Jul 2020 17:45:04 GMT
//    Content-Length : 77859
//    Date : Tue, 21 Jul 2020 02:59:06 GMT
//    x-cos-hash-crc64ecma : 14377791042765251108
//    Content-Type : image/jpeg



    /** 表示该服务器支持范围请求 Accept-Ranges : bytes */
    public String acceptRanges;
    /** 服务器类型 Server : nginx */
    public String server;
    /** 缓存验证MD5  ETag : "4df4d61142e773a16769473cf2654b71" */
    public String eTag;
    /** 文件类型 Content-Type : image/jpeg */
    public String contentType;
    /** 文件长度  Content-Length : 77859 */
    public long contentLength;
}
