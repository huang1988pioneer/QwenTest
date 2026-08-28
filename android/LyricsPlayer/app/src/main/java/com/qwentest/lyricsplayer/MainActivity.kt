package com.qwentest.lyricsplayer

import android.app.Activity
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var player: MediaPlayer
    private var prepared = false
    private var currentLine = -1
    private var dragging = false

    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            update()
            handler.postDelayed(this, 100)
        }
    }

    private lateinit var lyricsScroll: ScrollView
    private lateinit var lyricsContainer: LinearLayout
    private lateinit var seekBar: SeekBar
    private lateinit var timeLabel: TextView
    private lateinit var playBtn: Button
    private val avatars = LinkedHashMap<String, ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        lyricsScroll = findViewById(R.id.lyricsScroll)
        lyricsContainer = findViewById(R.id.lyricsContainer)
        seekBar = findViewById(R.id.seekBar)
        timeLabel = findViewById(R.id.timeLabel)
        playBtn = findViewById(R.id.playBtn)

        avatars["avatar_feng"] = findViewById(R.id.avatar_feng)
        avatars["avatar_tu"] = findViewById(R.id.avatar_tu)
        avatars["avatar_ya"] = findViewById(R.id.avatar_ya)
        avatars["avatar_yu"] = findViewById(R.id.avatar_yu)
        for ((key, iv) in avatars) {
            iv.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            iv.clipToOutline = true
            setAvatarState(key, false)
        }

        for ((i, line) in LyricsData.lines.withIndex()) {
            val tv = TextView(this)
            tv.text = line.text
            tv.gravity = Gravity.CENTER
            tv.setTextColor(Color.parseColor("#8F7FB0"))
            tv.textSize = 18f
            tv.setPadding(0, dp(9), 0, dp(9))
            tv.setOnClickListener {
                if (!prepared) return@setOnClickListener
                player.seekTo(LyricsData.lines[i].timeMs)
                if (!player.isPlaying) player.start()
            }
            lyricsContainer.addView(tv)
        }

        try {
            player = MediaPlayer()
            val afd = assets.openFd("song.mp3")
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            player.setOnPreparedListener {
                prepared = true
                seekBar.max = player.duration
                handler.post(tick)
            }
            player.setOnCompletionListener {
                it.seekTo(0)
                it.pause()
            }
            player.prepareAsync()
        } catch (e: Exception) {
            timeLabel.text = "找不到 song.mp3"
            playBtn.isEnabled = false
        }

        playBtn.setOnClickListener {
            if (!prepared) return@setOnClickListener
            if (player.isPlaying) player.pause() else player.start()
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {}
            override fun onStartTrackingTouch(sb: SeekBar?) { dragging = true }
            override fun onStopTrackingTouch(sb: SeekBar?) {
                dragging = false
                if (prepared) player.seekTo(sb?.progress ?: 0)
            }
        })
    }

    private fun update() {
        if (!prepared) return
        val pos = player.currentPosition
        var c = -1
        for (i in LyricsData.lines.indices) {
            if (pos >= LyricsData.lines[i].timeMs) c = i else break
        }
        if (c != currentLine) {
            currentLine = c
            restyle()
            lightAvatars()
            if (c >= 0) centerOn(c)
        }
        if (!dragging) seekBar.progress = pos
        timeLabel.text = "${fmt(pos)} / ${fmt(player.duration)}"
        playBtn.text = if (player.isPlaying) "⏸" else "▶"
    }

    private fun restyle() {
        for (i in 0 until lyricsContainer.childCount) {
            val tv = lyricsContainer.getChildAt(i) as TextView
            when {
                i == currentLine -> {
                    tv.setTextColor(Color.WHITE)
                    tv.textSize = 24f
                    tv.setTypeface(null, Typeface.BOLD)
                }
                i < currentLine -> {
                    tv.setTextColor(Color.parseColor("#5D4F80"))
                    tv.textSize = 18f
                    tv.setTypeface(null, Typeface.NORMAL)
                }
                else -> {
                    tv.setTextColor(Color.parseColor("#8F7FB0"))
                    tv.textSize = 18f
                    tv.setTypeface(null, Typeface.NORMAL)
                }
            }
        }
    }

    private fun lightAvatars() {
        val text = if (currentLine >= 0) LyricsData.lines[currentLine].text else ""
        for ((key, name) in LyricsData.characterNames) setAvatarState(key, text.contains(name))
    }

    private fun setAvatarState(key: String, active: Boolean) {
        val iv = avatars[key] ?: return
        iv.alpha = if (active) 1f else 0.5f
        iv.foreground = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(Color.TRANSPARENT)
            setStroke(dp(3), Color.parseColor(if (active) "#FFD75E" else "#4A3768"))
        }
    }

    private fun centerOn(index: Int) {
        val child = lyricsContainer.getChildAt(index) ?: return
        val max = (lyricsContainer.height - lyricsScroll.height).coerceAtLeast(0)
        val target = (child.top - lyricsScroll.height / 2 + child.height / 2).coerceIn(0, max)
        lyricsScroll.smoothScrollTo(0, target)
    }

    private fun fmt(ms: Int): String {
        val s = ms / 1000
        return "${s / 60}:${String.format("%02d", s % 60)}"
    }

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        if (::player.isInitialized) player.release()
        super.onDestroy()
    }
}
