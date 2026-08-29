package com.qwentest.lyricsplayer

import android.app.Activity
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowInsets
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView

class MainActivity : Activity() {

    private lateinit var player: MediaPlayer
    private var prepared = false
    private var songIndex = -1
    private lateinit var song: Song
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
    private lateinit var songTitle: TextView
    private lateinit var songSubtitle: TextView
    private lateinit var tabSong1: Button
    private lateinit var tabSong2: Button
    private lateinit var tabSong3: Button
    private lateinit var tabSong4: Button
    private lateinit var tabSong5: Button
    private lateinit var tabSong6: Button
    private lateinit var tabSong7: Button
    private lateinit var tabSong8: Button
    private lateinit var tabSong9: Button
    private val avatars = LinkedHashMap<String, ImageView>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // targetSdk 35 起強制 edge-to-edge，內容會畫到系統列後方：
        // 頂部讓出狀態列、底部播放列讓出導覽列，避免被系統按鍵遮住。
        val rootLayout = findViewById<LinearLayout>(R.id.rootLayout)
        val playerBar = findViewById<LinearLayout>(R.id.playerBar)
        rootLayout.setOnApplyWindowInsetsListener { v, insets ->
            val top: Int
            val bottom: Int
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bars = insets.getInsets(WindowInsets.Type.systemBars())
                top = bars.top
                bottom = bars.bottom
            } else {
                @Suppress("DEPRECATION")
                top = insets.systemWindowInsetTop
                @Suppress("DEPRECATION")
                bottom = insets.systemWindowInsetBottom
            }
            v.setPadding(0, top, 0, 0)
            playerBar.setPadding(
                playerBar.paddingLeft, playerBar.paddingTop,
                playerBar.paddingRight, dp(12) + bottom
            )
            WindowInsets.CONSUMED
        }

        lyricsScroll = findViewById(R.id.lyricsScroll)
        lyricsContainer = findViewById(R.id.lyricsContainer)
        seekBar = findViewById(R.id.seekBar)
        timeLabel = findViewById(R.id.timeLabel)
        playBtn = findViewById(R.id.playBtn)
        songTitle = findViewById(R.id.songTitle)
        songSubtitle = findViewById(R.id.songSubtitle)
        tabSong1 = findViewById(R.id.tabSong1)
        tabSong2 = findViewById(R.id.tabSong2)
        tabSong3 = findViewById(R.id.tabSong3)
        tabSong4 = findViewById(R.id.tabSong4)
        tabSong5 = findViewById(R.id.tabSong5)
        tabSong6 = findViewById(R.id.tabSong6)
        tabSong7 = findViewById(R.id.tabSong7)
        tabSong8 = findViewById(R.id.tabSong8)
        tabSong9 = findViewById(R.id.tabSong9)

        avatars["avatar_feng"] = findViewById(R.id.avatar_feng)
        avatars["avatar_tu"] = findViewById(R.id.avatar_tu)
        avatars["avatar_ya"] = findViewById(R.id.avatar_ya)
        avatars["avatar_yu"] = findViewById(R.id.avatar_yu)
        avatars["avatar_miao"] = findViewById(R.id.avatar_miao)
        for ((key, iv) in avatars) {
            iv.outlineProvider = object : ViewOutlineProvider() {
                override fun getOutline(view: View, outline: Outline) {
                    outline.setOval(0, 0, view.width, view.height)
                }
            }
            iv.clipToOutline = true
        }

        tabSong1.setOnClickListener { loadSong(0) }
        tabSong2.setOnClickListener { loadSong(1) }
        tabSong3.setOnClickListener { loadSong(2) }
        tabSong4.setOnClickListener { loadSong(3) }
        tabSong5.setOnClickListener { loadSong(4) }
        tabSong6.setOnClickListener { loadSong(5) }
        tabSong7.setOnClickListener { loadSong(6) }
        tabSong8.setOnClickListener { loadSong(7) }
        tabSong9.setOnClickListener { loadSong(8) }

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

        loadSong(0)
    }

    private fun loadSong(index: Int) {
        if (index == songIndex) return
        songIndex = index
        song = LyricsData.songs[index]

        prepared = false
        handler.removeCallbacks(tick)
        playBtn.text = "▶"
        currentLine = -1
        dragging = false
        seekBar.progress = 0
        timeLabel.text = "0:00 / 0:00"

        songTitle.text = song.title
        songSubtitle.text = song.subtitle
        styleTab(tabSong1, index == 0)
        styleTab(tabSong2, index == 1)
        styleTab(tabSong3, index == 2)
        styleTab(tabSong4, index == 3)
        styleTab(tabSong5, index == 4)
        styleTab(tabSong6, index == 5)
        styleTab(tabSong7, index == 6)
        styleTab(tabSong8, index == 7)
        styleTab(tabSong9, index == 8)

        lyricsContainer.removeAllViews()
        for ((i, line) in song.lines.withIndex()) {
            val tv = TextView(this)
            tv.text = line.text
            tv.gravity = Gravity.CENTER
            tv.setTextColor(Color.parseColor("#8F7FB0"))
            tv.textSize = 18f
            tv.setPadding(0, dp(9), 0, dp(9))
            tv.setOnClickListener {
                if (!prepared) return@setOnClickListener
                player.seekTo(song.lines[i].timeMs)
                if (!player.isPlaying) player.start()
            }
            lyricsContainer.addView(tv)
        }
        lyricsScroll.scrollTo(0, 0)
        for ((key, _) in avatars) setAvatarState(key, false)

        try {
            if (!::player.isInitialized) {
                player = MediaPlayer()
                player.setOnCompletionListener {
                    it.seekTo(0)
                    it.pause()
                }
            } else {
                player.reset()
            }
            val afd = assets.openFd(song.audioFile)
            player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
            afd.close()
            player.setOnPreparedListener {
                prepared = true
                seekBar.max = player.duration
                handler.post(tick)
            }
            player.prepareAsync()
            playBtn.isEnabled = true
        } catch (e: Exception) {
            timeLabel.text = "找不到 ${song.audioFile}"
            playBtn.isEnabled = false
        }
    }

    private fun styleTab(btn: Button, active: Boolean) {
        btn.background = GradientDrawable().apply {
            cornerRadius = dp(16).toFloat()
            setColor(Color.parseColor(if (active) "#FFD75E" else "#2A1C48"))
            setStroke(dp(1), Color.parseColor(if (active) "#FFD75E" else "#4A3768"))
        }
        btn.setTextColor(Color.parseColor(if (active) "#241436" else "#C9B6E4"))
        btn.setTypeface(null, if (active) Typeface.BOLD else Typeface.NORMAL)
    }

    private fun update() {
        if (!prepared) return
        val pos = player.currentPosition
        var c = -1
        for (i in song.lines.indices) {
            if (pos >= song.lines[i].timeMs) c = i else break
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
        val text = if (currentLine >= 0) song.lines[currentLine].text else ""
        val all = song.allWords.any { text.contains(it) }
        for ((key, _) in avatars) {
            val name = song.characterNames[key]
            val ch = song.charTriggers[key]
            val kws = song.characterKeywords[key]
            val excl = song.characterExclude[key]
            val excluded = excl != null && excl.any { text.contains(it) }
            val personal = (name != null && text.contains(name))
                || (ch != null && text.contains(ch))
                || (kws != null && kws.any { text.contains(it) })
            val active = all || (!excluded && personal)
            setAvatarState(key, active)
        }
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
