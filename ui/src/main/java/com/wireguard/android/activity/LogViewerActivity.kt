/*
 * Copyright © 2020 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.wireguard.android.R
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

import java.nio.charset.StandardCharsets

class LogViewerActivity: AppCompatActivity() {

    private lateinit var logAdapter: LogEntryAdapter
    private var logLines = arrayListOf<String>()
    private var process: Process? = null
    private var stdout: BufferedReader? = null
    private var thread: Thread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.log_viewer_activity)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        logAdapter = LogEntryAdapter()
        findViewById<RecyclerView>(R.id.log_viewer_recycler).apply {
            layoutManager = LinearLayoutManager(context)
            adapter = logAdapter
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, LinearLayoutManager.VERTICAL))
        }
        thread = Thread { startStreamingLog() }
        thread?.start()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            stdout?.close()
        } catch (_: IOException) {
            // This activity's going away regardless, so exceptions don't particularly matter anymore.
        }
        process?.destroy()
        thread?.interrupt()
        stdout = null
        process = null
        thread = null
    }

    private fun startStreamingLog() {
        val builder = ProcessBuilder().command("logcat", "-b", "all", "-v", "threadtime", "*:V")
        builder.environment()["LC_ALL"] = "C"
        try {
            process = builder.start()
        } catch (e: IOException) {
            e.printStackTrace()
            return
        }
        stdout = BufferedReader(InputStreamReader(process!!.inputStream, StandardCharsets.UTF_8))
        var line: String
        while(stdout!!.readLine().also { line = it } != null) {
            logLines.add(line)
            runOnUiThread { logAdapter.notifyDataSetChanged() }
        }
    }

    inner class LogEntryAdapter : RecyclerView.Adapter<LogEntryAdapter.ViewHolder>() {

        inner class ViewHolder(val textView: TextView, var isSingleLine: Boolean = true) : RecyclerView.ViewHolder(textView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val textView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.log_viewer_entry, parent, false) as TextView
            return ViewHolder(textView)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.apply {
                setSingleLine()
                text = logLines[position]
                setOnClickListener {
                    isSingleLine = !holder.isSingleLine
                    holder.isSingleLine = !holder.isSingleLine
                }
            }
        }

        override fun getItemCount() = logLines.size
    }
}
