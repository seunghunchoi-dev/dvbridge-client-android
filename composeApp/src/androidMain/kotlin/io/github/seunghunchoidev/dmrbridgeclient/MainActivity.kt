// SPDX-License-Identifier: MIT
// Copyright (c) 2026 최승훈 (Choi Seunghun, 6K2KZR)
package io.github.seunghunchoidev.dmrbridgeclient

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/** 안드로이드 화면: 공통 화면(App) + 권한 요청·볼륨 버튼 말하기·화면 켜 두기 */
class MainActivity : ComponentActivity() {
    private lateinit var model: UiModel
    private var pendingConnect: (() -> Unit)? = null

    private val permLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        if (r[Manifest.permission.RECORD_AUDIO] == false) model.toast = "마이크 권한이 없으면 듣기만 할 수 있습니다"
        pendingConnect?.invoke()
        pendingConnect = null
    }

    private fun hasMic() = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    private fun connect(host: String, port: Int) {
        val need = mutableListOf<String>()
        if (!hasMic()) need += Manifest.permission.RECORD_AUDIO
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED)
            need += Manifest.permission.POST_NOTIFICATIONS
        val go = { RadioService.connect(this, host, port) }
        if (need.isEmpty()) go() else { pendingConnect = go; permLauncher.launch(need.toTypedArray()) }
    }

    private fun keepOn(on: Boolean) {
        if (on) window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) else window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = AndroidStore(this)
        val radio = RadioHolder.get(this)
        model = UiModel(store, radio, UiHooks(
            connect = ::connect,
            disconnect = { RadioService.stop(this) },
            canRecord = ::hasMic,
            showVolumeKeyOption = true,
            keepScreenOn = ::keepOn,
        ))
        keepOn(store.getBoolean("keep_screen_on", false))
        val platform = RadioHolder.platform(this)
        setContent { App(model, platform) }
    }

    // 볼륨 아래 버튼 = 말하기 (설정에서 켰을 때, 화면이 열려 있을 때만)
    private var keyDown = false
    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (model.volumeKeyPtt && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (event.repeatCount == 0 && !keyDown) { keyDown = true; model.pttPress() }
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (model.volumeKeyPtt && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            if (keyDown) { keyDown = false; model.pttRelease() }
            return true
        }
        return super.onKeyUp(keyCode, event)
    }
}
