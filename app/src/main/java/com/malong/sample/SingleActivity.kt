package com.malong.sample

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.malong.moses.Constants
import com.malong.moses.Download
import com.malong.moses.DownloadListener
import com.malong.moses.DownloadTask
import com.malong.moses.utils.MimeTypeUtils
import com.malong.moses.utils.SpeedCalculator
import com.malong.sample.base.BaseSampleActivity
import com.malong.sample.util.DemoUtil
import java.io.File


/** 最简单下载演示 */
class SingleActivity : BaseSampleActivity() {
    companion object {
        private const val TAG = "【SingleActivity】"
    }

    private var task: DownloadTask? = null
    private var mActivity: Activity? = null

    /** 计算下载速度 */
    private var mSpeedCalculator: SpeedCalculator = SpeedCalculator()

    /** 展示下载状态，上面的 */
    private var statusTv: TextView? = null

    /** 展示下载的按钮文字 下面的*/
    private var actionTv: TextView? = null

    /** 控制下载的按钮 下面的*/
    private var actionView: View? = null

    /** 下载进度条 */
    private var progressBar: ProgressBar? = null

    /** 打开下载文件的按钮 */
    private var openBtn: Button? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mActivity = this
        setContentView(R.layout.activity_single)
        statusTv = findViewById<View>(R.id.statusTv) as TextView
        progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
        actionView = findViewById(R.id.actionView)
        actionTv = findViewById<View>(R.id.actionTv) as TextView
        openBtn = findViewById<Button>(R.id.openBtn)

        init()
    }

    override fun titleRes(): Int = R.string.single_common_download_title

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
        val filename = "single-test"
        val url = Constants.BASE_URL + Constants.IMAGE_NAME
//        val url =
//            "http://downapp.baidu.com/baidusearch/AndroidPhone/11.25.0.11/1/757p/20200712134622/baidusearch_AndroidPhone_11-25-0-11_757p.apk?responseContentDisposition=attachment%3Bfilename%3D%22baidusearch_AndroidPhone_757p.apk%22&responseContentType=application%2Fvnd.android.package-archive&request_id=1595472387_5127736889&type=static"
//        val url = "https://cdn.llscdn.com/yy/files/xs8qmxn8-lls-LLS-5.8-800-20171207-111607.apk"
        val parentFile = DemoUtil.getParentFile(this)
        task = DownloadTask.Builder()
            .setDescription_path(parentFile.toString())
            .setFileName(filename)
            .setDownloadUrl(url)
            .build()
    }

    /** 获取该任务之前的下载信息 */
    private fun initStatus() {
        task = Download.convertDownloadInfo(mActivity, task)// 若之前下载过，会更新task信息
        DemoUtil.calcProgressToView(
            progressBar,
            task!!.current_bytes,
            task!!.total_bytes
        )// 显示当前的下载进度
        statusTv?.text = convertStatus(task!!.status)// 显示当前的下载状态
        // 更新下载按钮文案
        when (task!!.status) {
            DownloadTask.STATUS_RUNNING -> {
                actionTv?.text = "暂停"
            }
            DownloadTask.STATUS_SUCCESS -> {
                actionTv?.text = "删除"
            }
            else -> {
                actionTv?.text = "下载"
            }
        }
    }

    /** 配置点击事件 */
    private fun initAction() {
        actionView?.setOnClickListener {
            task?.let {
                // let：表示 task 不为null的条件下，才会去执行let函数体.it 指向 task
                when (it.status) {
                    DownloadTask.STATUS_RUNNING -> {
                        // 下载中 to pause
                        Download.pauseDownload(mActivity, task)
                    }
                    DownloadTask.STATUS_SUCCESS -> {
                        // 下载完成 to 删除
                        Download.deleteDownload(mActivity, task)
                        //                    mListener.unregister()
                    }
                    else -> {
                        // else to 下载
                        task = Download.doDownload(mActivity, task)
                        mListener.register(mActivity, task!!.id)
                    }
                }
            }
        }

        openBtn?.setOnClickListener {
            task?.let {
                if (it.status == DownloadTask.STATUS_SUCCESS) {
                    openFile(mActivity!!, it.destination_path + it.fileName)
                } else {
                    Toast.makeText(mActivity!!,"没下载完呢",Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /** 下载状态监听 */
    private var mListener: DownloadListener = object : DownloadListener() {
        /** 状态变更监听 */
        override fun onStatusChange(uri: Uri?, status: Int) {
            Log.d(TAG, "status=$status")
            task?.status = status
            if (status == DownloadTask.STATUS_RUNNING) {
                // to pause
                actionTv?.text = "暂停"
            } else if (status == DownloadTask.STATUS_SUCCESS) {
                statusTv?.text = convertStatus(status)
                actionTv?.text = "删除"
            } else {
                // to start
                statusTv?.text = convertStatus(status)
                actionTv?.setText("下载")
            }
        }

        var preProcess: Long = 0

        /** 进度变更监听 */
        override fun onProcessChange(uri: Uri?, cur: Long, length: Long) {
            Log.d(TAG, "【DownloadListener】status=$cur length=$length")
            mSpeedCalculator.downloading(cur - preProcess)
            preProcess = cur
            val percent: Float = cur * 1f / length
            progressBar!!.progress = (percent * progressBar!!.max).toInt()
            if (task?.status == DownloadTask.STATUS_RUNNING) {
                val curProcess: String = SpeedCalculator.humanReadableBytes(cur, false)
                val totalProcess: String = SpeedCalculator.humanReadableBytes(length, false)
                val speed: String = mSpeedCalculator.speed()
                val progressStatusWithSpeed = "$curProcess / $totalProcess（$speed）"
                statusTv?.text = progressStatusWithSpeed
            }

        }
    }

    /** 状态码 to 文字 */
    private fun convertStatus(status: Int): String {
        when (status) {
            DownloadTask.STATUS_PENDING -> return "准备"
            DownloadTask.STATUS_RUNNING -> return "下载中"
            DownloadTask.STATUS_SUCCESS -> return "下载完成"
            DownloadTask.STATUS_PAUSE -> return "暂停中"
            DownloadTask.STATUS_FAIL -> return "下载失败"
            DownloadTask.STATUS_CANCEL -> return "删除完成"
        }
        return status.toString()
    }

    fun openFile(context: Context, file: String?) {
        try {
            val intent = Intent()
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.action = Intent.ACTION_VIEW
            intent.setDataAndType(Uri.fromFile(File(file)), MimeTypeUtils.getMIMEType(file))
            context.startActivity(intent)
            Intent.createChooser(intent, "请选择对应的软件打开该附件！")
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "sorry附件不能打开，请下载相关软件！", Toast.LENGTH_SHORT).show()
        }
    }
}
