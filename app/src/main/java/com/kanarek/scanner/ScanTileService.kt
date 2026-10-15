package com.kanarek.scanner

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.TileService
import com.kanarek.HomeActivity

class ScanTileService : TileService() {
    override fun onClick() {
        super.onClick()
        if (isLocked) unlockAndRun { openScanner() } else openScanner()
    }

    private fun openScanner() {
        val target = Intent(this, HomeActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(HomeActivity.EXTRA_PAGE, HomeActivity.PAGE_TOOLS)
            putExtra(HomeActivity.EXTRA_SCAN, true)
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(
                this, 11, target, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(target)
        }
    }
}
