package com.example

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import com.example.platform.ConflictAlertNotifier
import com.example.platform.ConflictWarningEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ConflictAlertNotifierRobolectricTest {

    @Test
    fun notifyConflictDisplaysWarningToastAndNotification() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        shadowOf(context).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
        )

        val notifier = ConflictAlertNotifier(context)
        val event = ConflictWarningEvent(
            id = "evt_1",
            title = "Venue Double-Booking",
            message = "Warning: Turing 204 is already booked for MATH-240.",
            overlapCount = 0,
            venueClashCount = 1
        )

        notifier.notifyConflict(event)

        val latestToast = ShadowToast.getTextOfLatestToast()
        assertNotNull(latestToast)
        assertTrue(latestToast.contains("Turing 204"))

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNotificationManager = shadowOf(notificationManager)
        val posted = shadowNotificationManager.getNotification(ConflictAlertNotifier.CONFLICT_NOTIFICATION_ID)
        assertNotNull(posted)
        assertEquals(ConflictAlertNotifier.CHANNEL_ID, posted.channelId)
    }
}
