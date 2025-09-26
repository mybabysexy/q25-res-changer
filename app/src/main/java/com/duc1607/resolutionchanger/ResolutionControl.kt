package com.duc1607.resolutionchanger

import android.content.Context
import android.os.IBinder
import android.util.Log
import androidx.core.content.edit

object ResolutionControl {
    const val PREFS_NAME = "resolution_tile_prefs"
    const val KEY_INDEX = "current_index"

    fun resolutions(): List<Resolution> = DefaultResolutions.common

    fun getCurrentIndex(context: Context): Int =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getInt(KEY_INDEX, 0)

    fun setCurrentIndex(context: Context, index: Int) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putInt(KEY_INDEX, index) }
    }

    fun getWindowManagerInterface(context: Context): Any? {
        return try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val windowManagerBinder = getServiceMethod.invoke(null, Context.WINDOW_SERVICE) as IBinder
            val windowManagerStubClass = Class.forName("android.view.IWindowManager\$Stub")
            val asInterfaceMethod = windowManagerStubClass.getMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.invoke(null, windowManagerBinder)
        } catch (e: Exception) {
            Log.e("ResolutionControl", "Failed to get IWindowManager: ${e.message}")
            null
        }
    }

    // Advances to next preset and applies it. Returns Pair<success, message>
    fun toggleNext(context: Context): Pair<Boolean, String> {
        val list = resolutions()
        if (list.isEmpty()) return Pair(false, "No resolutions configured")
        val nextIndex = (getCurrentIndex(context) + 1) % list.size
        val res = list[nextIndex]
        setCurrentIndex(context, nextIndex)
        val wm = getWindowManagerInterface(context)
        return changeResolutionWithIWindowManager(wm, res.width, res.height)
    }
}

