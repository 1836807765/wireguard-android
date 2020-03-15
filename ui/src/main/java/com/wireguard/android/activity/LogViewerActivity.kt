/*
 * Copyright © 2020 WireGuard LLC. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.wireguard.android.activity

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface.BOLD
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textview.MaterialTextView
import com.wireguard.android.R
import com.wireguard.android.util.resolveAttribute
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

import java.nio.charset.StandardCharsets
import java.text.DateFormat
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Matcher
import java.util.regex.Pattern

class LogViewerActivity: AppCompatActivity() {

    private lateinit var logAdapter: LogEntryAdapter
    private var logLines = arrayListOf<LogLine>()
    private var process: Process? = null
    private var stdout: BufferedReader? = null
    private var thread: Thread? = null
    private val year by lazy {
        val yearFormatter: DateFormat = SimpleDateFormat("yyyy", Locale.US)
        yearFormatter.format(Date())
    }

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
            val logLine = parseLine(line)
            if (logLine != null) {
                logLines.add(logLine)
                runOnUiThread { logAdapter.notifyDataSetChanged() }
            }
        }
    }

    private fun parseTime(timeStr: String): Date? {
        val formatter: DateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
        return try {
            formatter.parse("$year-$timeStr")
        } catch (e: ParseException) {
            null
        }
    }

    private fun parseLine(line: String): LogLine? {
        val m: Matcher = THREADTIME_LINE.matcher(line)
        return if (m.matches()) LogLine(m.group(2).toInt(), m.group(3).toInt(), parseTime(m.group(1)), m.group(4), m.group(5), m.group(6)) else null
    }

    data class LogLine(val pid: Int, val tid: Int, val time: Date?, val level: String, val tag: String, val msg: String)

    companion object {
        /**
         * Match a single line of `logcat -v threadtime`, such as:
         *
         * <pre>05-26 11:02:36.886 5689 5689 D AndroidRuntime: CheckJNI is OFF.</pre>
         */
        private val THREADTIME_LINE: Pattern = Pattern.compile("^(\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}.\\d{3})(?:\\s+[0-9A-Za-z]+)?\\s+(\\d+)\\s+(\\d+)\\s+([A-Z])\\s+(.+?)\\s*: (.*)$")
    }

    inner class LogEntryAdapter : RecyclerView.Adapter<LogEntryAdapter.ViewHolder>() {

        inner class ViewHolder(val layout: View, var isSingleLine: Boolean = true) : RecyclerView.ViewHolder(layout)

        private fun levelToColor(context: Context, level: String): Int {
            return when (level) {
                "E" -> Color.RED
                "W" -> Color.YELLOW
                "I" -> Color.GREEN
                else -> context.resolveAttribute(android.R.attr.textColorPrimary)
            }
        }
        override fun getItemCount() = logLines.size

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.log_viewer_entry, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val line = logLines[position]
            val spannable = if (position > 0 && logLines[position - 1].tag == line.tag)
                SpannableString(line.msg)
            else
                SpannableString("${line.tag}: ${line.msg}").apply {
                    setSpan(StyleSpan(BOLD), 0, "${line.tag}:".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                    setSpan(ForegroundColorSpan(levelToColor(holder.layout.context, line.level)),
                            0, "${line.tag}:".length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                }
            holder.layout.apply {
                findViewById<MaterialTextView>(R.id.log_date).text = line.time.toString()
                findViewById<MaterialTextView>(R.id.log_msg).apply {
                    setSingleLine()
                    text = spannable
                    setOnClickListener {
                        isSingleLine = !holder.isSingleLine
                        holder.isSingleLine = !holder.isSingleLine
                    }
                }
            }
        }
    }
}
