package com.duc1607.resolutionchanger

import android.content.Context
import android.os.IBinder
import android.util.Log
import androidx.core.content.edit

object ResolutionControl {
    const val PREFS_NAME = "resolution_tile_prefs"
    const val KEY_INDEX = "current_index"
    const val KEY_TILE_SET = "tile_resolution_set"

    /** All resolutions shown in the UI (the "master" list). */
    fun allResolutions(): List<Resolution> = DefaultResolutions.all

    /**
     * Returns the set of resolution keys (e.g. "720x1280") that are checked for the tile.
     * Defaults to the first resolution if nothing has been saved yet.
     */
    fun getTileSet(context: Context): Set<String> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val saved = prefs.getStringSet(KEY_TILE_SET, null)
        if (saved != null) return saved
        // Default: first resolution in the list
        val default = allResolutions().firstOrNull()?.let { setOf(it.toKey()) } ?: emptySet()
        return default
    }

    fun setTileSet(context: Context, keys: Set<String>) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit { putStringSet(KEY_TILE_SET, keys) }
    }

    fun isCheckedForTile(context: Context, resolution: Resolution): Boolean =
        getTileSet(context).contains(resolution.toKey())

    fun setCheckedForTile(context: Context, resolution: Resolution, checked: Boolean) {
        val current = getTileSet(context).toMutableSet()
        if (checked) current.add(resolution.toKey()) else current.remove(resolution.toKey())
        setTileSet(context, current)
    }

    private fun Resolution.toKey() = "${width}x${height}"

    /** Resolutions used by the quick tile – only the checked ones. */
    fun resolutions(context: Context): List<Resolution> {
        val keys = getTileSet(context)
        val filtered = allResolutions().filter { keys.contains(it.toKey()) }
        return filtered.ifEmpty { allResolutions().take(1) }
    }

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

    // Advances to next preset and applies it.
    fun toggleNext(context: Context) {
        val list = resolutions(context)
        if (list.isEmpty()) return
        val currentIndex = getCurrentIndex(context).coerceIn(0, list.size - 1)
        val nextIndex = (currentIndex + 1) % list.size
        val res = list[nextIndex]
        setCurrentIndex(context, nextIndex)
        val wm = getWindowManagerInterface(context)
        return changeResolutionWithIWindowManager(wm, res.width, res.height)
    }
}

