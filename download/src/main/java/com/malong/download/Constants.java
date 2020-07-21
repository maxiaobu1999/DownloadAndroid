package com.malong.download;

public class Constants {
    public static final String BASE_URL = "https://maqinglong-1253423006.cos.ap-beijing-1.myqcloud.com/lib_download/";// mini_video/
    public static final String TIK_NAME = "mda-jk8du50gv2jwae5r.mp4";// 诛仙小说 size：4557690
    public static final String IMAGE_NAME = "mda-jk8du50gv2jwae5r00000.jpg";// 图片 size：	77859
    public static final String ZHU_XIAN_NAME = "诛仙.txt";// 诛仙小说 size：4557690
    public static boolean DEBUG = true;


    public static final String KEY_STATUS = "status";
    public static final String KEY_STATUS_CHANGE = "status_change";
    public static final String KEY_PROCESS_CHANGE = "process_change";
    public static final String KEY_PROCESS = "process";
    public static final String KEY_ID = "id";
    public static final String KEY_URI = "uri";
    public static final String KEY_PARTIAL_NUM = "partial_num";





    /** 数据库名称 */
    public static final String DB_NAME = "downloads.db";
    /** 数据库版本 */
    public static final int DB_VERSION = 2;
    /** 数据库表名 */
    public static final String DB_TABLE = "downloads";
    public static final String DB_PARTIAL_TABLE = "partial";

    /*==============================表列名BEGIN========================*/
    /** 主键自增 INTEGER */
    public static final String _ID = "_id";
    /** 下载地址 TEXT 必须*/
    public static final String COLUMN_DOWNLOAD_URL = "download_url";
    /** 保存地址 uri TEXT nullable*/
    public static final String COLUMN_DESTINATION_URI = "destination_uri";
    /** 保存地址 文件路径（不一定有，ROM P 媒体文件夹可能获取不到文件的实际路径） TEXT */
    public static final String COLUMN_DESTINATION_PATH = "destination_path";
    /** 文件的名称，没有起一个 TEXT 必须*/
    public static final String COLUMN_FILE_NAME = "file_name";
    /** 下载方式 : 重新下载 断点续传 差量下载 分片下载 INTEGER*/
    public static final String COLUMN_METHOD =   "method";





    /** 下载数据的MIME类型 TEXT */
    public static final String COLUMN_MIME_TYPE = "mime_type";
    /** 最后一次修改时间 BIGINT */
    public static final String COLUMN_LAST_MOD = "last_mod";
    /** 下载状态 暂停正在下载 INTEGER */
    public static final String COLUMN_STATUS = "status";
    /** 下载的文件总大小 BIGINT */
    public static final String COLUMN_TOTAL_BYTES = "total_bytes";
    /** 当前下载的文件大小 BIGINT */
    public static final String COLUMN_CURRENT_BYTES = "current_bytes";
    /** 请求头 map2json TEXT */
    public static final String COLUMN_HEADER = "header";
    /** 下载速度 TEXT */
    public static final String COLUMN_SPEED = "speed";
    /** 缓存校验md5 TEXT */
    public static final String COLUMN_ETAG = "etag";
    /** 什么状态可以下载 Wi-Fi 流量 漫游 INTEGER */
    public static final String COLUMN_ALLOWED_NETWORK = "allowed_network";
    /** 条件允许时，自动开始下载（资源预下载） BOOLEAN */
    public static final String COLUMN_AUTO_RESUME = "auto_resume";
    /** 下载优先级 INTEGER */
    public static final String COLUMN_PRIORITY = "priority";
    /** 下载中心可见，缓存目录下不应提供给外部 BOOLEAN */
    public static final String COLUMN_VISIBLE = "visible";
    /** 开启完整性校验，校验ETag  BOOLEAN */
    public static final String COLUMN_INTEGRITY = "integrity";
    /** 应用程序特定数据（预留，没想好干什么用）  TEXT */
    public static final String COLUMN_ENTITY = "entity";
    /** 应用程序特定数据（预留，没想好干什么用）  TEXT */
    public static final String COLUMN_DATA = "data";
    /** 使用分片  BOOLEAN */
    public static final String COLUMN_SEPARATE = "separate";
    /** 分片数量  INTEGER */
    public static final String COLUMN_SEPARATE_NUM = "separate_num";
    /** 已完成的分片数量  TEXT */
    public static final String COLUMN_SEPARATE_DONE = "separate_done";
    /** 该下载的标题  INTEGER */
    public static final String COLUMN_TITLE = "title";
    /** 下载描述  TEXT */
    public static final String COLUMN_DESCRIPTION = "description";
    /** 进行媒体扫描，下载完成后让别的应用可以发现文件  BOOLEAN */
    public static final String COLUMN_MEDIA_SCANNED = "media_scanned";
    /** 点通知跳哪里,类名全路径  TEXT */
    public static final String COLUMN_NOTIFICATION_CLASS = "notification_class";
    /** 跳转携带的数据  TEXT */
    public static final String COLUMN_NOTIFICATION_EXTRAS = "notification_extras";




    /** 外键  download表的ID INTEGER */
    public static final String PARTIAL_DOWNLOAD_ID= "download_id";
    /** 下载状态 暂停正在下载 INTEGER */
    public static final String PARTIAL_STATUS = "status";
    /** 分片索引 INTEGER */
    public static final String PARTIAL_NUM= "num";
    /** 当前进度 BIGINT */
    public static final String PARTIAL_CURRENT_BYTES= "current_bytes";
    /** 分片总大小 BIGINT */
    public static final String PARTIAL_TOTAL_BYTES = "total_bytes";

    /** 分片起点 BIGINT */
    public static final String PARTIAL_START_INDEX= "start_index";
    /** 分片终点点 BIGINT */
    public static final String PARTIAL_END_INDEX= "end_index";

    /** 下载地址 TEXT 必须*/
    public static final String PARTIAL_DOWNLOAD_URL = "download_url";
    /** 保存地址 uri TEXT nullable*/
    public static final String PARTIAL_DESTINATION_URI = "destination_uri";
    /** 保存地址 文件路径（不一定有，ROM P 媒体文件夹可能获取不到文件的实际路径） TEXT */
    public static final String PARTIAL_DESTINATION_PATH = "destination_path";
    /** 文件的名称，没有起一个 TEXT 必须*/
    public static final String PARTIAL_FILE_NAME = "file_name";




}
