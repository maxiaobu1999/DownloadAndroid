/*
 * Copyright (c) 2017 LingoChamp Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.malong.sample.util.queue

import android.util.Log
import android.util.SparseArray


internal class QueueListener
//    : DownloadListener1()
{

//    private val holderMap = SparseArray<QueueViewHolder>()
//
//    fun bind(task: DownloadTask, holder: QueueViewHolder) {
//        Log.i(TAG, "bind " + task.id + " with " + holder)
//        // replace.
//        val size = holderMap.size()
//        for (i in 0 until size) {
//            if (holderMap.valueAt(i) === holder) {
//                holderMap.removeAt(i)
//                break
//            }
//        }
//        holderMap.put(task.id, holder)
//    }
//
//    fun resetInfo(task: DownloadTask, holder: QueueViewHolder) {
//        // task name
//        val taskName = TagUtil.getTaskName(task)
//        holder.nameTv.text = taskName
//
//        // process references
//        val status = TagUtil.getStatus(task)
//        if (status != null) {
//            //   started
//            holder.statusTv.text = status
//            if (status == EndCause.COMPLETED.name) {
//                holder.progressBar.progress = holder.progressBar.max
//            } else {
//                val total = TagUtil.getTotal(task)
//                if (total == 0L) {
//                    holder.progressBar.progress = 0
//                } else {
//                    ProgressUtil.calcProgressToViewAndMark(holder.progressBar,
//                        TagUtil.getOffset(task), total, false)
//                }
//            }
//        } else {
//            // non-started
//            val statusOnStore = StatusUtil.getStatus(task)
//            TagUtil.saveStatus(task, statusOnStore.toString())
//            if (statusOnStore == StatusUtil.Status.COMPLETED) {
//                holder.statusTv.text = EndCause.COMPLETED.name
//                holder.progressBar.progress = holder.progressBar.max
//            } else {
//                when (statusOnStore) {
//                    StatusUtil.Status.IDLE -> holder.statusTv.setText(R.string.state_idle)
//                    StatusUtil.Status.PENDING -> holder.statusTv.setText(R.string.state_pending)
//                    StatusUtil.Status.RUNNING -> holder.statusTv.setText(R.string.state_running)
//                    else -> holder.statusTv.setText(R.string.state_unknown)
//                }
//
//                if (statusOnStore == StatusUtil.Status.UNKNOWN) {
//                    holder.progressBar.progress = 0
//                } else {
//                    val info = StatusUtil.getCurrentInfo(task)
//                    if (info != null) {
//                        TagUtil.saveTotal(task, info.totalLength)
//                        TagUtil.saveOffset(task, info.totalOffset)
//                        ProgressUtil.calcProgressToViewAndMark(holder.progressBar,
//                            info.totalOffset, info.totalLength, false)
//                    } else {
//                        holder.progressBar.progress = 0
//                    }
//                }
//            }
//        }
//    }
//
//    fun clearBoundHolder() = holderMap.clear()
//
//    override fun taskStart(
//        task: DownloadTask,
//        model: Listener1Assist.Listener1Model
//    ) {
//        val status = "taskStart"
//        TagUtil.saveStatus(task, status)
//
//        val holder = holderMap.get(task.id) ?: return
//
//        holder.statusTv.text = status
//    }
//
//    override fun retry(task: DownloadTask, cause: ResumeFailedCause) {
//        val status = "retry"
//        TagUtil.saveStatus(task, status)
//
//        val holder = holderMap.get(task.id) ?: return
//
//        holder.statusTv.text = status
//    }
//
//    override fun connected(
//        task: DownloadTask,
//        blockCount: Int,
//        currentOffset: Long,
//        totalLength: Long
//    ) {
//        val status = "connected"
//        TagUtil.saveStatus(task, status)
//        TagUtil.saveOffset(task, currentOffset)
//        TagUtil.saveTotal(task, totalLength)
//
//        val holder = holderMap.get(task.id) ?: return
//
//        holder.statusTv.text = status
//
//        ProgressUtil.calcProgressToViewAndMark(
//            holder.progressBar,
//            currentOffset,
//            totalLength,
//            false
//        )
//    }
//
//    override fun progress(task: DownloadTask, currentOffset: Long, totalLength: Long) {
//        val status = "progress"
//        TagUtil.saveStatus(task, status)
//        TagUtil.saveOffset(task, currentOffset)
//
//        val holder = holderMap.get(task.id) ?: return
//
//        holder.statusTv.text = status
//
//        Log.i(TAG, "progress " + task.id + " with " + holder)
//        ProgressUtil.updateProgressToViewWithMark(holder.progressBar, currentOffset, false)
//    }
//
//    override fun taskEnd(
//        task: DownloadTask,
//        cause: EndCause,
//        realCause: Exception?,
//        model: Listener1Assist.Listener1Model
//    ) {
//        val status = cause.toString()
//        TagUtil.saveStatus(task, status)
//
//        Log.w(TAG, "${task.url} end with: $cause")
//        val holder = holderMap.get(task.id) ?: return
//
//        holder.statusTv.text = status
//        if (cause == EndCause.COMPLETED) {
//            holder.progressBar.progress = holder.progressBar.max
//        }
//    }
//
//    companion object {
//        private const val TAG = "QueueListener"
//    }
}