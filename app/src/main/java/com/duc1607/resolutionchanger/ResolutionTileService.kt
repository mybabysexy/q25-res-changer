package com.duc1607.resolutionchanger

import android.content.Context
import android.graphics.drawable.Icon
import android.os.IBinder
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import android.widget.Toast
import androidx.core.content.edit

class ResolutionTileService : TileService() {
    private val resolutions = DefaultResolutions.common
    private val prefs by lazy {
        getSharedPreferences("resolution_tile_prefs", MODE_PRIVATE)
    }
    private var currentIndex: Int
        get() = prefs.getInt("current_index", 0)
        set(value) = prefs.edit { putInt("current_index", value) }

    override fun onStartListening() {
        super.onStartListening()
        updateTileLabel()
    }

    override fun onClick() {
        super.onClick()
        val nextIndex = (currentIndex + 1) % resolutions.size
        val resolution = resolutions[nextIndex]
        currentIndex = nextIndex
        updateTileLabel()
        // Try to change resolution
        val (success, message) = changeResolutionWithIWindowManager(
            getWindowManagerInterface(),
            resolution.width,
            resolution.height
        )
        if (!success) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTileLabel() {
        val tile = qsTile ?: return
        val resolution = resolutions[currentIndex]
        tile.label = "${resolution.width}x${resolution.height}"
        tile.state = Tile.STATE_ACTIVE
        tile.icon = Icon.createWithResource(this, android.R.drawable.ic_menu_zoom)
        tile.updateTile()
    }

    private fun getWindowManagerInterface(): Any? {
        // Use reflection as in MainActivity
        return try {
            val serviceManagerClass = Class.forName("android.os.ServiceManager")
            val getServiceMethod = serviceManagerClass.getMethod("getService", String::class.java)
            val windowManagerBinder =
                getServiceMethod.invoke(null, Context.WINDOW_SERVICE) as IBinder
            val windowManagerStubClass = Class.forName("android.view.IWindowManager\$Stub")
            val asInterfaceMethod =
                windowManagerStubClass.getMethod("asInterface", IBinder::class.java)
            asInterfaceMethod.invoke(null, windowManagerBinder)
        } catch (e: Exception) {
            Log.e("ResolutionTileService", "Failed to get IWindowManager: ${e.message}")
            null
        }
    }
}
