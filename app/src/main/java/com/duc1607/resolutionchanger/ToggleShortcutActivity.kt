package com.duc1607.resolutionchanger

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast

class ToggleShortcutActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val action = intent?.action
        val data: Uri? = intent?.data
        val extras = intent?.extras

        val result = when (action) {
            Intent.ACTION_VIEW -> {
                // Support data URIs like: app://resolutionchanger/preset/{index}
                if (data != null && data.scheme == "app" && data.host == "resolutionchanger") {
                    val segments = data.pathSegments
                    if (segments.size >= 2 && segments[0] == "preset") {
                        val index = segments[1].toIntOrNull()
                        val list = ResolutionControl.resolutions()
                        if (index != null && index in list.indices) {
                            val res = list[index]
                            ResolutionControl.setCurrentIndex(this, index)
                            val wm = ResolutionControl.getWindowManagerInterface(this)
                            changeResolutionWithIWindowManager(wm, res.width, res.height)
                        } else {
                            Pair(false, "Invalid preset index")
                        }
                    } else {
                        // If URI not recognized, default to toggle
                        ResolutionControl.toggleNext(this)
                    }
                } else {
                    // No data or different scheme/host: default to toggle
                    ResolutionControl.toggleNext(this)
                }
            }
            "com.duc1607.resolutionchanger.action.SET_RESOLUTION" -> {
                val width = extras?.getInt("width", -1) ?: -1
                val height = extras?.getInt("height", -1) ?: -1
                if (width > 0 && height > 0) {
                    val wm = ResolutionControl.getWindowManagerInterface(this)
                    changeResolutionWithIWindowManager(wm, width, height)
                } else {
                    Pair(false, "Missing width/height extras")
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
                    Pair(false, "Invalid preset index")
                }
            }
            else -> {
                // Default behavior: toggle to the next preset
                ResolutionControl.toggleNext(this)
            }
        }

        finish()
    }
}
