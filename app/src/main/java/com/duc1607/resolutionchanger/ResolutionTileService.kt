package com.duc1607.resolutionchanger

import android.graphics.drawable.Icon
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.widget.Toast

class ResolutionTileService : TileService() {
    private val resolutions get() = ResolutionControl.resolutions()

    override fun onStartListening() {
        super.onStartListening()
        updateTileLabel()
    }

    override fun onClick() {
        super.onClick()
        val (success, message) = ResolutionControl.toggleNext(this)
        updateTileLabel()
        if (!success) {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateTileLabel() {
        val tile = qsTile ?: return
        val index = ResolutionControl.getCurrentIndex(this)
        val resolution = resolutions[index]
        tile.label = "${resolution.width}x${resolution.height}"
        tile.state = Tile.STATE_ACTIVE
        tile.icon = Icon.createWithResource(this, android.R.drawable.ic_menu_zoom)
        tile.updateTile()
    }
}
