package com.qwentest.lyricsplayer

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder

/**
 * 背景播放的前景服務：只負責 MediaSession 與媒體通知（含 Android 10+ 的拖曳時間軸），
 * 音檔本身仍由 MainActivity 的 MediaPlayer 播放；狀態由 Activity 的 100ms tick 推過來，
 * 通知列上的播放／暫停按鍵與時間軸拖曳則透過 [command]／[seekToMs] 回交給 Activity 消費。
 */
class PlaybackService : Service() {

    companion object {
        private const val CHANNEL_ID = "playback"
        private const val NOTIFICATION_ID = 1
        private const val ACTION_TOGGLE = "com.qwentest.lyricsplayer.TOGGLE"

        /** 通知列按鍵／媒體鍵的待處理指令，Activity 的 tick 讀取後以 consumeCommand() 清除 */
        @Volatile
        var command: String? = null

        /** 通知列時間軸拖曳的目標位置（毫秒），Activity 讀取後以 consumeSeek() 清除 */
        @Volatile
        var seekToMs = -1L

        @Volatile
        var active = false
            private set

        @Volatile
        var instance: PlaybackService? = null
            private set

        /** 由 Activity 在播放開始／歌曲切換時呼叫：啟動前景服務並帶入曲目資訊 */
        fun push(context: android.content.Context, title: String, subtitle: String) {
            val i = Intent(context, PlaybackService::class.java)
                .putExtra("title", title)
                .putExtra("subtitle", subtitle)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(i)
            else context.startService(i)
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, PlaybackService::class.java))
        }

        fun consumeCommand(): String? = command.also { command = null }
        fun consumeSeek(): Long = seekToMs.also { seekToMs = -1L }
    }

    private var session: MediaSession? = null
    private var lastTitle: String? = null
    private var lastSubtitle: String? = null
    private var lastPlaying = false
    private var durationMs = 0L
    private var positionMs = 0L
    private var lastStatePushAt = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val i = intent ?: return START_NOT_STICKY
        val title = i.getStringExtra("title") ?: lastTitle ?: return START_NOT_STICKY
        val subtitle = i.getStringExtra("subtitle") ?: ""

        if (session == null) {
            createSession()
            createChannel()
            active = true
            instance = this
        }

        val trackChanged = title != lastTitle
        lastTitle = title
        lastSubtitle = subtitle
        if (trackChanged) {
            durationMs = 0L
            positionMs = 0L
        }
        startForegroundWithNotification()
        return START_NOT_STICKY
    }

    private fun createSession() {
        session = MediaSession(this, "LyricsPlayer").apply {
            setCallback(object : MediaSession.Callback() {
                override fun onPlay() { command = "toggle" }
                override fun onPause() { command = "toggle" }
                override fun onSeekTo(pos: Long) { seekToMs = pos }
            })
            isActive = true
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "背景播放", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
    }

    /** Activity 每個 tick 推一次。Metadata 只在曲目變更時更新；播放狀態（含位置）最多每秒推送一次——
     *  系統會用「位置＋速度」自行插值讓通知列時間軸前進，過度頻繁更新會讓狀態列／通知不停重繪（看起來像畫面一直刷新） */
    fun updatePlayback(positionMs: Int, durationMs: Int, playing: Boolean) {
        if (!active) return
        this.positionMs = positionMs.toLong()
        if (durationMs > 0) this.durationMs = durationMs.toLong()

        val now = android.os.SystemClock.elapsedRealtime()
        val playingChanged = playing != lastPlaying
        if (playingChanged || now - lastStatePushAt >= 1000) {
            lastStatePushAt = now
            updateMetadata()
            updateSessionState(playing)
        }
        if (playingChanged) {
            lastPlaying = playing
            startForegroundWithNotification()
        }
    }

    private fun updateMetadata() {
        val s = session ?: return
        s.setMetadata(android.media.MediaMetadata.Builder()
            .putString(android.media.MediaMetadata.METADATA_KEY_TITLE, lastTitle ?: "")
            .putString(android.media.MediaMetadata.METADATA_KEY_ARTIST, lastSubtitle ?: "")
            .putLong(android.media.MediaMetadata.METADATA_KEY_DURATION, durationMs)
            .build())
    }

    private fun updateSessionState(playing: Boolean) {
        val s = session ?: return
        s.setPlaybackState(PlaybackState.Builder()
            .setActions(PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE
                or PlaybackState.ACTION_SEEK_TO or PlaybackState.ACTION_STOP)
            .setState(
                if (playing) PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,
                positionMs, if (playing) 1f else 0f)
            .build())
    }

    private fun startForegroundWithNotification() {
        updateMetadata()
        updateSessionState(lastPlaying)

        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val toggle = PendingIntent.getBroadcast(
            this, 0, Intent(this, PlaybackActionReceiver::class.java).setAction(ACTION_TOGGLE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(lastTitle)
            .setContentText(lastSubtitle)
            .setContentIntent(openApp)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .addAction(
                if (lastPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                if (lastPlaying) "暫停" else "播放", toggle)

        val style = Notification.MediaStyle().setMediaSession(session?.sessionToken)
        builder.setStyle(style.setShowActionsInCompactView(0))

        val notification = builder.build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        active = false
        instance = null
        session?.release()
        session = null
        super.onDestroy()
    }
}

/** 通知列播放／暫停按鍵的轉接器：把指令放進 PlaybackService，由 Activity 的 tick 消費 */
class PlaybackActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
        PlaybackService.command = "toggle"
    }
}
