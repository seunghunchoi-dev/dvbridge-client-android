// SPDX-License-Identifier: MIT
// Copyright (c) 2026 최승훈 (Choi Seunghun, 6K2KZR)
package io.github.seunghunchoidev.dmrbridgeclient

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/** 연결을 붙잡고 있는 포그라운드 서비스: 화면이 꺼지거나 다른 앱을 써도 수신을 계속함 (본체는 공통 Radio) */
class RadioService : Service() {
    companion object {
        const val ACTION_CONNECT = "connect"
        const val ACTION_STOP = "stop"
        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
        private const val CHANNEL = "radio"
        private const val NOTI_ID = 1

        fun connect(ctx: Context, host: String, port: Int) {
            val i = Intent(ctx, RadioService::class.java).setAction(ACTION_CONNECT).putExtra(EXTRA_HOST, host).putExtra(EXTRA_PORT, port)
            ContextCompat.startForegroundService(ctx, i)
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, RadioService::class.java).setAction(ACTION_STOP))
        }
    }

    private lateinit var radio: Radio
    private val main = Handler(Looper.getMainLooper())
    private var wake: PowerManager.WakeLock? = null
    private var wifi: WifiManager.WifiLock? = null
    private var lastNoti = ""

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        radio = RadioHolder.get(this)
        radio.onChanged = { main.post { updateNoti() } }
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(NotificationChannel(CHANNEL, "연결 상태", NotificationManager.IMPORTANCE_LOW))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                goForeground("연결 중…")
                acquireLocks()
                radio.connect(intent.getStringExtra(EXTRA_HOST) ?: "", intent.getIntExtra(EXTRA_PORT, 32001))
            }
            ACTION_STOP -> { shutdown(); stopSelf() }
            else -> if (radio.connState == UsrpClient.State.OFF) stopSelf()
        }
        return START_NOT_STICKY
    }

    private fun goForeground(text: String) {
        val mic = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        var type = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            type = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            if (mic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        ServiceCompat.startForeground(this, NOTI_ID, buildNoti(text), type)
        lastNoti = text
    }

    private fun buildNoti(text: String): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val stop = PendingIntent.getService(this, 1, Intent(this, RadioService::class.java).setAction(ACTION_STOP), PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.app_name))
            .setContentText(text)
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "연결 끊기", stop)
            .build()
    }

    private fun updateNoti() {
        val s = radio.state.value
        val text = when {
            s.tx -> "송신 중"
            s.rx -> "수신 중: ${s.peer?.let { if (it.notice) "브릿지 공지" else it.call.ifEmpty { it.src.toString() } } ?: ""}"
            s.conn == UsrpClient.State.ONLINE -> "연결됨" + if (s.tg > 0) " · ${if (s.priv) "개인" else "TG"} ${s.tg}" else ""
            s.conn == UsrpClient.State.NO_REPLY -> "서버 응답 없음 - 다시 시도 중"
            else -> "연결 중…"
        }
        if (text == lastNoti || s.conn == UsrpClient.State.OFF) return
        lastNoti = text
        getSystemService(NotificationManager::class.java).notify(NOTI_ID, buildNoti(text))
    }

    private fun acquireLocks() {
        if (wake == null) wake = (getSystemService(POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DMRClient:radio").apply { acquire() }
        if (wifi == null) {
            @Suppress("DEPRECATION")
            wifi = (applicationContext.getSystemService(WIFI_SERVICE) as WifiManager)
                .createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "DMRClient:radio").apply { acquire() }
        }
    }

    private fun shutdown() {
        radio.disconnect()
        wake?.let { if (it.isHeld) it.release() }; wake = null
        wifi?.let { if (it.isHeld) it.release() }; wifi = null
    }

    override fun onDestroy() {
        shutdown()
        radio.onChanged = null
        main.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
