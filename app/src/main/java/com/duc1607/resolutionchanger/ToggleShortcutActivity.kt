package com.duc1607.resolutionchanger

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.util.Log

class ToggleShortcutActivity : Activity() {
    companion object {
        private const val MIN_DUP_INTERVAL_MS = 750L // ignore duplicates within this window
        private var lastIntentSignature: String? = null
        private var lastIntentTimestamp: Long = 0L
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action
        val data: Uri? = intent?.data
        val extras = intent?.extras

        // Build a stable signature of this launch (action + data + sorted extras keys/values)
        val extrasSignature = extras?.let {
            val keys = try { it.keySet() } catch (e: Exception) { emptySet<String>() }
            keys.sorted().joinToString(";") { k -> "$k=${it.get(k)}" }
        } ?: "no_extras"
        val signature = listOf(action ?: "(null)", data?.toString() ?: "(null)", extrasSignature).joinToString("|")

        val now = SystemClock.elapsedRealtime()
        if (signature == lastIntentSignature && (now - lastIntentTimestamp) < MIN_DUP_INTERVAL_MS) {
            finish()
            return
        }
        lastIntentSignature = signature
        lastIntentTimestamp = now

        // Force unparcel for consistent logging of extras
        val extrasMap = extras?.let {
            val keys = try { it.keySet() } catch (e: Exception) { emptySet<String>() }
            keys.associateWith { k -> it.get(k) }
        }

        when (action) {
            Intent.ACTION_VIEW -> {
                ResolutionControl.toggleNext(this)
            }
            "com.duc1607.resolutionchanger.action.SET_RESOLUTION" -> {
                val width = extras?.getInt("width", -1) ?: -1
                val height = extras?.getInt("height", -1) ?: -1
                if (width > 0 && height > 0) {
                    val wm = ResolutionControl.getWindowManagerInterface(this)
                    changeResolutionWithIWindowManager(wm, width, height)
                } else {
                    Log.w("ToggleShortcutActivity", "Missing width/height extras; width=$width height=$height")
                }
            }
            "com.duc1607.resolutionchanger.action.SET_PRESET_INDEX" -> {
                val index = extras?.getInt("index", -1) ?: -1
                val list = ResolutionControl.resolutions()
                if (index in list.indices) {
                    val res = list[index]
                    ResolutionControl.setCurrentIndex(this, index)
                    val wm = ResolutionControl.getWindowManagerInterface(this)
                    changeResolutionWithIWindowManager(wm, res.width, res.height)
                } else {
                    Log.w("ToggleShortcutActivity", "Invalid preset index: $index")
                }
            }
            else -> {
                ResolutionControl.toggleNext(this)
            }
        }

        finish()
    }
}
