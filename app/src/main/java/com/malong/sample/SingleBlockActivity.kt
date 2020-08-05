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
import com.malong.moses.block.BlockInfo
import com.malong.moses.listener.BlockListener
import com.malong.moses.utils.FileUtils
import com.malong.moses.utils.SpeedCalculator
import com.malong.sample.base.BaseSampleActivity
import com.malong.sample.util.DemoUtil

/** 分片+续传 最简单下载演示 */
class SingleBlockActivity : BaseSampleActivity() {
    companion object {
        @Suppress("unused")
        private const val TAG = "【SingleBlockActivity】"
        private const val BLOCK_NUM = 5
    }

    private var mRequest: Request? = null
    private var mBlockList: ArrayList<BlockInfo?> = ArrayList(BLOCK_NUM)
    private var mActivity: Activity? = null

    /** 计算下载速度 */
    private var mSpeedCalculator: SpeedCalculator = SpeedCalculator()

    /** 总下载信息 */
    private var totalInfoTv: TextView? = null

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

        val request = buildRequest()
        // 获取该任务之前的下载信息
        mRequest = Download.queryDownloadInfo(mActivity, request)// 若之前下载过，会更新task信息
        updateHost()

        // 查询分片信息
        val partialList = Download.queryPartialInfoList(mActivity, mRequest!!.id)
        for (item in partialList) {
            mBlockList.add(item)
            updatePartial(item.num, item.status, item.current_bytes, item.total_bytes)
        }

        initAction()

    }

    override fun titleRes(): Int = R.string.single_partial_download_title

    override fun onDestroy() {
        super.onDestroy()
        mListener.unregister()
        Download.pauseDownload(mActivity, mRequest)
    }

    /** 配置下载任务信息 */
    private fun buildRequest(): Request {
        val url = Constants.BASE_URL + Constants.TIK_NAME3
        val parentFile = DemoUtil.getParentFile(this)
        val filename = FileUtils.getFileNameFromUrl(url)
        return Request.Builder()
            .setDescription_path(parentFile.toString())
            .setFileName(filename!!)
            .setDownloadUrl(url)
            .setMethod(Request.METHOD_PARTIAL)
            .setSeparate_num(BLOCK_NUM)
            .setMin_progress_time(70)// 设置进度通知间隔，控制下载速度
            .build()
    }

    /** 上次的进度 */
    var preProcess: Long = 0

    /** 刷新总进度显示内容 */
    private fun updateHost() {
        // 进度条
        val percent: Float = mRequest!!.current_bytes * 1f / mRequest!!.total_bytes
        progressBar!!.progress = (percent * progressBar!!.max).toInt()
        if (mRequest!!.status == Request.STATUS_SUCCESS) {
            progressBar!!.progress = (progressBar!!.max)
        }
        // 拼接文案
        val curProcess: String = SpeedCalculator.humanReadableBytes(mRequest!!.current_bytes, false)
        val totalProcess: String = SpeedCalculator.humanReadableBytes(mRequest!!.total_bytes, false)
        val speed: String = mSpeedCalculator.speed()
        var progressStatusWithSpeed = "$curProcess / $totalProcess（$speed）"
        progressStatusWithSpeed =
            "总状态:${convertStatus(mRequest!!.status)}。进度=$progressStatusWithSpeed"
        totalInfoTv?.text = progressStatusWithSpeed
    }


    /** 配置点击事件 */
    private fun initAction() {
        // 下载
        downloadActionView?.setOnClickListener {
            // let：表示 task 不为null的条件下，才会去执行let函数体.it 指向 task
            mRequest?.let {
                val downloadId = Download.doDownload(mActivity, it)
                mRequest!!.id = downloadId
                mListener.register(mActivity, downloadId)// 主监听
            }
        }
        // 暂停
        pauseActionView?.setOnClickListener {
            mRequest?.let {
                Download.pauseDownload(mActivity, mRequest)
            }
        }
        // 删除
        deleteActionView?.setOnClickListener {
            mRequest?.let {
                mListener.unregister()
                Download.deleteDownload(mActivity, mRequest)
                mRequest = buildRequest()
                mRequest!!.status = Request.STATUS_CANCEL
                updateHost()
                for (i in 0 until BLOCK_NUM) {
                    updatePartial(i, BlockInfo.STATUS_CANCEL, 0, 0)
                }
            }
        }
        // 打开下载文件
        openActionTv?.setOnClickListener {
            mRequest?.let {
                if (it.status == Request.STATUS_SUCCESS) {
                    DemoUtil.openFile(mActivity!!, it.destination_path + it.fileName)
                } else {
                    Toast.makeText(mActivity!!, "没下载完呢", Toast.LENGTH_LONG).show()
                }
            }
        }
    }


    /** 刷新分片进度显示内容 */
    private fun updatePartial(
        index: Int,
        status: Int?,
        progress: Long?,
        length: Long?
    ) {
        if (mBlockList.size < index + 1) mBlockList.add(index, BlockInfo())
        val blockInfo: BlockInfo = mBlockList[index]!!
        if (status != null) blockInfo.status = status
        if (progress != null) blockInfo.current_bytes = progress
        if (length != null) blockInfo.total_bytes = length


        val progressBarId = resources.getIdentifier(
            "progressBar" + (index + 1),
            "id", mActivity!!.packageName
        )
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
        // 计算速度
        calculator.downloading(blockInfo.current_bytes - blockPreProgressList[index])
        // 显示当前分片的下载进度
        DemoUtil.calcProgressToView(
            partialProgressBar,
            blockInfo.current_bytes,
            blockInfo.total_bytes
        )
        // 记录
        blockPreProgressList[index] = blockInfo.current_bytes
        blockPreLengthList[index] = blockInfo.total_bytes

        // 状态变化
        blockPreStatusList[index] = blockInfo.status

        val speed = blockSpeedCalculatorList[index]!!.speed()
        // 显示当前分片的下载信息
        val curProcess: String = SpeedCalculator.humanReadableBytes(blockInfo.current_bytes, false)
        val totalProcess: String = SpeedCalculator.humanReadableBytes(blockInfo.total_bytes, false)
        val progressStatusWithSpeed = "$curProcess / $totalProcess（$speed）"
        val content = "Task" + index + ": " +
                "状态=${convertStatus(blockInfo.status)}。进度=$progressStatusWithSpeed"
        pdTv.text = content


    }

    /** 下载状态监听 */
    private var mListener: BlockListener = object : BlockListener() {
        /** 状态变更监听 */
        override fun onStatusChange(status: Int) {
            Log.d(SingleBlockActivity.TAG, "status=$status")
            mRequest!!.status = status
            updateHost()
        }

        /** 进度变更监听 */
        override fun onProgress(cur: Long, length: Long) {
            Log.d(SingleBlockActivity.TAG, "cur=$cur length=$length")
            mRequest!!.current_bytes = cur
            mRequest!!.total_bytes = length
            // 速度
            mSpeedCalculator.downloading(mRequest!!.current_bytes - preProcess)
            preProcess = mRequest!!.current_bytes

            updateHost()
        }

        override fun onBlockStatusChange(index: Int, status: Int) {
            super.onBlockStatusChange(index, status)
            Log.d(SingleBlockActivity.TAG, "onBlockStatusChange：index=$index;status=$status")
            updatePartial(index, status, null, null)
        }


        /** 分片进度变更监听 */
        override fun onBlockProcessChange(index: Int, progress: Long, length: Long) {
            super.onBlockProcessChange(index, progress, length)
            Log.d(SingleBlockActivity.TAG, "onBlockProcessChange：index=$index；progress=$progress")
            updatePartial(index, null, progress, length)
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
            Request.STATUS_NONE -> return "没有下载过"
        }
        return status.toString()
    }
}
