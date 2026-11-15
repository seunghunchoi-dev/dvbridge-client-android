// SPDX-License-Identifier: MIT
// Copyright (c) 2026 최승훈 (Choi Seunghun, 6K2KZR)
package io.github.seunghunchoidev.dvbridgeclient

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.os.Build
import android.provider.Settings
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.roundToInt

/** SharedPreferences "settings" — 예전 안드로이드판과 같은 이름·키라 설정이 그대로 이어짐 */
class AndroidStore(ctx: Context) : KeyValueStore {
    private val p = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE)
    override fun getString(key: String, def: String) = p.getString(key, def) ?: def
    override fun getInt(key: String, def: Int) = try { p.getInt(key, def) } catch (_: Exception) { def }
    override fun getFloat(key: String, def: Float) = try { p.getFloat(key, def) } catch (_: Exception) { def }
    override fun getBoolean(key: String, def: Boolean) = try { p.getBoolean(key, def) } catch (_: Exception) { def }
    override fun put(key: String, value: Any) {
        val e = p.edit()
        when (value) {
            is String -> e.putString(key, value)
            is Int -> e.putInt(key, value)
            is Float -> e.putFloat(key, value)
            is Boolean -> e.putBoolean(key, value)
            else -> e.putString(key, value.toString())
        }
        e.apply()
    }
}

class AndroidPlatform(ctx: Context) : PlatformInfo {
    override val appLabel = "DVBridge Client $APP_VERSION (Android ${Build.VERSION.RELEASE})"
    override val defaultDevice = DeviceInfo.detect(ctx)
    override val isDesktop = false
    override val dataDir: java.io.File = ctx.filesDir
}

/**
 * 장비명 (예: "Galaxy A17") — 서버가 BM 위치 칸과 이름표에 씀. BM에는 영문·숫자만 들어가므로 한글 이름은 쓰지 않음.
 *  1) 삼성 원래 모델명 (System "device_name")  2) 사용자 기기 이름의 영문 모델 부분 ("승훈의 A17" → Galaxy A17)  3) 모델 코드
 */
object DeviceInfo {
    private fun isAscii(s: String) = s.isNotBlank() && s.all { it.code in 0x20..0x7E }

    fun detect(ctx: Context): String {
        val cr = ctx.contentResolver
        val samsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
        try {
            val n = Settings.System.getString(cr, "device_name")?.trim()
            if (n != null && isAscii(n)) return n
        } catch (_: Exception) {}
        try {
            val user = Settings.Global.getString(cr, Settings.Global.DEVICE_NAME)?.trim().orEmpty()
            if (isAscii(user)) return user
            val m = Regex("""([A-Z]{1,2}\s?\d{1,3}[A-Za-z]*(\s(Ultra|Plus|FE|Edge))?|Z\s?(Flip|Fold)\s?\d+)""").find(user)
            if (m != null) return (if (samsung) "Galaxy " else "") + m.value.trim()
        } catch (_: Exception) {}
        return Build.MODEL.trim().ifEmpty { "Android" }
    }
}

/**
 * 휴대폰 마이크·스피커 ⇄ 8kHz 16bit 160샘플
 *  - 재생: 지터 버퍼 60ms(3프레임) 모은 뒤, 1초 넘게 밀리면 오래된 것 버림. USAGE_MEDIA라 스피커로
 *  - 녹음: 말하기 중에만 마이크를 엶. 8kHz가 안 되면 16k/48k로 열고 변환
 */
class AndroidAudio : AudioBackend {
    companion object { private const val RATE = 8000; private const val FRAME = 160; private const val PREBUFFER = 3 }

    @Volatile override var volume = 1.0f
    @Volatile override var micGain = 1.0f

    private var track: AudioTrack? = null
    private val playQ = LinkedBlockingQueue<ShortArray>()
    @Volatile private var flush = false
    @Volatile private var playing = false
    private var playThread: Thread? = null

    @Synchronized override fun startPlayback() {
        if (track != null) return
        val min = AudioTrack.getMinBufferSize(RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val t = AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setSampleRate(RATE).setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                .setEncoding(AudioFormat.ENCODING_PCM_16BIT).build())
            .setBufferSizeInBytes(maxOf(min, RATE * 2 / 5))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        t.play()
        track = t
        playing = true
        playThread = Thread({ playLoop(t) }, "audio-play").apply { start() }
    }

    @Synchronized override fun stopPlayback() {
        playing = false
        playThread?.interrupt()
        playThread?.join(500)
        playThread = null
        track?.run { try { stop() } catch (_: Exception) {}; release() }
        track = null
        playQ.clear()
    }

    override fun playFrame(pcm: ShortArray) {
        playQ.offer(pcm)
        while (playQ.size > 50) playQ.poll()
    }

    override fun playEnd() { flush = true }

    private fun playLoop(t: AudioTrack) {
        var active = false
        val out = ShortArray(FRAME)
        try {
            while (playing) {
                if (!active) {
                    if (playQ.size >= PREBUFFER || (flush && playQ.isNotEmpty())) active = true
                    else { Thread.sleep(10); continue }
                }
                val f = playQ.poll(100, TimeUnit.MILLISECONDS)
                if (f == null) { active = false; flush = false; continue }
                val v = volume
                for (i in 0 until FRAME) out[i] = (f[i] * v).roundToInt().coerceIn(-32768, 32767).toShort()
                t.write(out, 0, FRAME)
            }
        } catch (_: InterruptedException) {}
    }

    @Volatile private var recording = false
    private var recThread: Thread? = null

    @SuppressLint("MissingPermission")
    override fun startCapture(onFrame: (ShortArray) -> Unit, onLevel: (Float) -> Unit): Boolean {
        if (recording) return true
        var rec: AudioRecord? = null
        var rate = 0
        for (r in intArrayOf(8000, 16000, 48000, 44100)) {
            val min = AudioRecord.getMinBufferSize(r, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (min <= 0) continue
            val a = try {
                AudioRecord(MediaRecorder.AudioSource.MIC, r, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(min, r / 5 * 2))
            } catch (_: Exception) { null }
            if (a != null && a.state == AudioRecord.STATE_INITIALIZED) { rec = a; rate = r; break }
            a?.release()
        }
        if (rec == null) return false
        recording = true
        val r = rec
        recThread = Thread({ recLoop(r, rate, onFrame, onLevel) }, "audio-rec").apply { start() }
        return true
    }

    override fun stopCapture() {
        recording = false
        recThread?.join(500)
        recThread = null
    }

    private fun recLoop(rec: AudioRecord, rate: Int, onFrame: (ShortArray) -> Unit, onLevel: (Float) -> Unit) {
        val rs = Resampler(rate, RATE)
        val chunk = rate / 50
        val raw = ShortArray(chunk)
        val fin = FloatArray(chunk)
        val acc = ArrayList<Float>(FRAME * 4)
        var peak = 0f
        var nf = 0
        try {
            rec.startRecording()
            while (recording) {
                val n = rec.read(raw, 0, chunk)
                if (n <= 0) continue
                val g = micGain
                for (i in 0 until n) fin[i] = raw[i] / 32768f * g
                rs.process(fin, n, acc)
                while (acc.size >= FRAME) {
                    val pcm = ShortArray(FRAME)
                    for (i in 0 until FRAME) {
                        val v = acc[i].coerceIn(-1f, 1f)
                        peak = maxOf(peak, abs(v))
                        pcm[i] = (v * 32767f).roundToInt().toShort()
                    }
                    acc.subList(0, FRAME).clear()
                    onFrame(pcm)
                    if (++nf >= 3) { onLevel(peak); peak = 0f; nf = 0 }
                }
            }
        } catch (_: Exception) {
        } finally {
            try { rec.stop() } catch (_: Exception) {}
            rec.release()
            onLevel(0f)
        }
    }
}

/** 화면과 서비스가 같은 Radio 하나를 씀 */
object RadioHolder {
    @Volatile private var radio: Radio? = null
    @Volatile private var platform: PlatformInfo? = null
    fun platform(ctx: Context): PlatformInfo = platform ?: synchronized(this) { platform ?: AndroidPlatform(ctx.applicationContext).also { platform = it } }
    fun get(ctx: Context): Radio = radio ?: synchronized(this) {
        radio ?: Radio(AndroidStore(ctx.applicationContext), platform(ctx), AndroidAudio()).also { radio = it }
    }
}
