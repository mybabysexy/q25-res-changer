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
        ResolutionControl.toggleNext(this)
        updateTileLabel()
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
