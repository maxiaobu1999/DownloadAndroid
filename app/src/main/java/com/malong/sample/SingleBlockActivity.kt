package com.malong.sample

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.util.SparseArray
import android.util.SparseIntArray
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.util.set
import com.malong.moses.Constants
import com.malong.moses.Download
import com.malong.moses.Request
import com.malong.moses.listener.BlockListener
import com.malong.moses.block.BlockInfo
import com.malong.moses.utils.FileUtils
import com.malong.moses.utils.SpeedCalculator
import com.malong.sample.base.BaseSampleActivity
import com.malong.sample.util.DemoUtil

/** 分片+续传 最简单下载演示 */
class SingleBlockActivity : BaseSampleActivity() {
    companion object {
        @Suppress("unused")
        private const val TAG = "【SinglePartialActivity】"
        private const val BLOCK_NUM = 5

    }

    private var task: Request? = null

    //    /** 分片数据 */
//    private var partialList: List<PartialInfo>? = null
//    private var observerList: List<DownloadContentObserver>? = null
    private var mActivity: Activity? = null

    /** 计算下载速度 */
    private var mSpeedCalculator: SpeedCalculator = SpeedCalculator()

    /** 总下载信息 */
    private var totalInfoTv: TextView? = null

//    /** 展示下载的按钮文字 下面的*/
//    private var actionTv: TextView? = null

    /** 下载按钮*/
    private var downloadActionView: View? = null

    /** 暂停按钮*/
    private var pauseActionView: View? = null

    /** 删除按钮*/
    private var deleteActionView: View? = null

    /** 下载进度条 */
    private var progressBar: ProgressBar? = null

    /** 打开下载文件的按钮 */
    private var openActionTv: View? = null

    private var blockPreProgressList = SparseArray<Long>()
    private var blockPreLengthList = SparseArray<Long>()
    private var blockPreStatusList = SparseIntArray()
    private var blockSpeedCalculatorList = SparseArray<SpeedCalculator?>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = this
        setContentView(R.layout.activity_single_block)
        totalInfoTv = findViewById<View>(R.id.totalInfoTv) as TextView
        progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        downloadActionView = findViewById(R.id.downloadActionView)
        pauseActionView = findViewById(R.id.pauseActionView)
        deleteActionView = findViewById(R.id.deleteActionView)
        openActionTv = findViewById(R.id.openActionTv)

        init()
    }

    override fun titleRes(): Int = R.string.single_partial_download_title

    override fun onDestroy() {
        super.onDestroy()
        mListener.unregister()
        Download.pauseDownload(mActivity, task)
    }

    private fun init() {
        initTask()
        initStatus()
        initAction()
    }

    /** 配置下载任务信息 */
    private fun initTask() {
//        val url = Constants.BASE_URL + Constants.IMAGE_NAME
        val url =
            "http://downapp.baidu.com/baidusearch/AndroidPhone/11.25.0.11/1/757p/20200712134622/baidusearch_AndroidPhone_11-25-0-11_757p.apk?responseContentDisposition=attachment%3Bfilename%3D%22baidusearch_AndroidPhone_757p.apk%22&responseContentType=application%2Fvnd.android.package-archive&request_id=1595472387_5127736889&type=static"
        val filename = FileUtils.getFileNameFromUrl(url)
//        val url = "https://cdn.llscdn.com/yy/files/xs8qmxn8-lls-LLS-5.8-800-20171207-111607.apk"
        val parentFile = DemoUtil.getParentFile(this)
        task = Request.Builder()
            .setDescription_path(parentFile.toString())
            .setFileName(filename!!)
            .setDownloadUrl(url)
            .setMethod(Request.METHOD_PARTIAL)
            .setSeparate_num(BLOCK_NUM)
            .build()
    }


    /** 获取该任务之前的下载信息 */
    private fun initStatus() {
        task = Download.queryDownloadInfo(mActivity, task)// 若之前下载过，会更新task信息
        // 显示当前的下载进度
        DemoUtil.calcProgressToView(progressBar, task!!.current_bytes, task!!.total_bytes)
        val assembleTotalContent =
            assembleTotalContent(task!!.status, task!!.current_bytes, task!!.total_bytes)
        totalInfoTv?.text = assembleTotalContent// 显示当前的下载状态

        // 查询分片信息
        val partialList = Download.queryPartialInfoList(mActivity, task!!.id)
        for (item in partialList) {
            updatePartial(item.num, item.current_bytes, item.total_bytes, item.status)
        }
    }

    /** 配置点击事件 */
    private fun initAction() {
        // 下载
        downloadActionView?.setOnClickListener {
            // let：表示 task 不为null的条件下，才会去执行let函数体.it 指向 task
            task?.let { it ->
                val downloadId = Download.doDownload(mActivity, it)
                task = Download.queryDownloadInfo(mActivity, downloadId)
                mListener.register(mActivity, task!!.id)// 主监听
            }
        }
        pauseActionView?.setOnClickListener {
            task?.let {
                Download.pauseDownload(mActivity, task)
            }
        }
        deleteActionView?.setOnClickListener {
            task?.let {
                Download.deleteDownload(mActivity, task)
                for (i in 0 until BLOCK_NUM) {
                    updatePartial(i, 0, 0, BlockInfo.STATUS_CANCEL)
                }

            }
        }
        openActionTv?.setOnClickListener {
            task?.let {
                if (it.status == Request.STATUS_SUCCESS) {
                    DemoUtil.openFile(mActivity!!, it.destination_path + it.fileName)
                } else {
                    Toast.makeText(mActivity!!, "没下载完呢", Toast.LENGTH_LONG).show()
                }
            }
        }


    }


    private fun updatePartial(index: Int, progress: Long?, length: Long?, status: Int?) {
        val progressBarId =
            resources.getIdentifier("progressBar" + (index + 1), "id", mActivity!!.packageName)
        val pdId =
            resources.getIdentifier(
                "pb" + (index + 1) + "_info_tv",
                "id",
                mActivity!!.packageName
            )
        val partialProgressBar = findViewById<ProgressBar>(progressBarId)// 进度条
        val pdTv = findViewById<TextView>(pdId)// 文字


        // 速度监听器
        var calculator = blockSpeedCalculatorList[index]
        if (calculator == null) {
            calculator = SpeedCalculator()
            blockSpeedCalculatorList[index] = calculator
        }
        if (blockPreProgressList[index] == null) {
            blockPreProgressList[index] = 0
        }
        // 进度变化
        if (progress != null && length != null) {
            // 计算速度
            calculator.downloading(progress - blockPreProgressList[index])
            // 显示当前分片的下载进度
            DemoUtil.calcProgressToView(partialProgressBar, progress, length)

            // 记录
            blockPreProgressList[index] = progress
            blockPreLengthList[index] = length

        }
        if (status != null) {
            // 状态变化
            blockPreStatusList[index] = status
        }

        val curStatus = blockPreStatusList[index]
        val curProgress = blockPreProgressList[index]
        val curLength = blockPreLengthList[index]
        val speed = blockSpeedCalculatorList[index]!!.speed()


        // 显示当前分片的下载信息
        val content = "Task$index: " +
                assemblePartialContent(curStatus, speed, curProgress, curLength)
        pdTv.text = content


    }

    private fun assemblePartialContent(
        status: Int,
        speed: String?,
        current_bytes: Long,
        total_bytes: Long
    ): String {
        val curProcess: String = SpeedCalculator.humanReadableBytes(current_bytes, false)
        val totalProcess: String = SpeedCalculator.humanReadableBytes(total_bytes, false)
        val progressStatusWithSpeed = "$curProcess / $totalProcess（$speed）"
        return "状态=${convertStatus(status)}。进度=$progressStatusWithSpeed"
    }

    private fun assembleTotalContent(status: Int, current_bytes: Long, total_bytes: Long): String {
        val curProcess: String = SpeedCalculator.humanReadableBytes(current_bytes, false)
        val totalProcess: String = SpeedCalculator.humanReadableBytes(total_bytes, false)
        val speed: String = mSpeedCalculator.speed()
        val progressStatusWithSpeed = "$curProcess / $totalProcess（$speed）"
        return "总状态:${convertStatus(task!!.status)}。进度=$progressStatusWithSpeed"
    }

    /** 下载状态监听 */
    private var mListener: BlockListener = object : BlockListener() {
        /** 状态变更监听 */
        override fun onStatusChange(status: Int) {
            Log.d(SingleBlockActivity.TAG, "status=$status")
            task?.status = status
        }

        /** 进度变更监听 */
        override fun onProgress(cur: Long, length: Long) {
            Log.d(SingleBlockActivity.TAG, "cur=$cur length=$length")
            mSpeedCalculator.downloading(cur - preProcess)
            preProcess = cur
            val percent: Float = cur * 1f / length
            progressBar!!.progress = (percent * progressBar!!.max).toInt()
            val progressStatusWithSpeed = assembleTotalContent(task!!.status, cur, length)
//                "下载任务：$status。$curProcess / $totalProcess（$speed）"
            totalInfoTv?.text = progressStatusWithSpeed
        }

        override fun onBlockStatusChange(index: Int, status: Int) {
            super.onBlockStatusChange(index, status)
            Log.d(SingleBlockActivity.TAG, "onBlockStatusChange：status=$status")
            updatePartial(index, null, null, status)
        }

        var preProcess: Long = 0

        /** 分片进度变更监听 */
        override fun onBlockProcessChange(index: Int, progress: Long, length: Long) {
            super.onBlockProcessChange(index, progress, length)
            Log.d(SingleBlockActivity.TAG, "onBlockProcessChange：index=$index；progress=$progress")
            updatePartial(index, progress, length, null)
        }
    }

    /** 状态码 to 文字 */
    private fun convertStatus(status: Int): String {
        when (status) {
            Request.STATUS_PENDING -> return "准备"
            Request.STATUS_RUNNING -> return "下载中"
            Request.STATUS_SUCCESS -> return "下载完成"
            Request.STATUS_PAUSE -> return "暂停中"
            Request.STATUS_FAIL -> return "下载失败"
            Request.STATUS_CANCEL -> return "删除完成"
        }
        return status.toString()
    }
}
