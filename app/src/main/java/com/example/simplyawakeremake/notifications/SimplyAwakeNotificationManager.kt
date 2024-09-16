package com.example.simplyawakeremake.notifications

import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.media3.ui.PlayerNotificationManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.example.simplyawakeremake.R
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The size of the large icon for the notification in pixels.
 */
const val NOTIFICATION_LARGE_ICON_SIZE = 144 // px

/**
 * The channel ID for the notification.
 */
const val NOW_PLAYING_CHANNEL_ID = "org.julienbirabent.android.simplyawakeremake"

/**
 * The notification ID.
 */
const val NOW_PLAYING_NOTIFICATION_ID = 0xb339 // Arbitrary number used to identify our notification

/**
 * Default options for Glide.
 */
private val glideOptions = RequestOptions()
    .fallback(R.drawable.ic_notification)
    .diskCacheStrategy(DiskCacheStrategy.DATA)


/**
 * A wrapper class for ExoPlayer's PlayerNotificationManager.
 * It sets up the notification shown to the user during audio playback and provides track metadata,
 * such as track title and icon image.
 * @param context The context used to create the notification.
 * @param sessionToken The session token used to build MediaController.
 * @param player The ExoPlayer instance.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class SimplyAwakeNotificationManager(
    private val context: Context,
    sessionToken: SessionToken,
    private val player: Player
) {
    private val notificationManager: PlayerNotificationManager

    init {

        val mediaController = MediaController.Builder(context, sessionToken).buildAsync()

        notificationManager = PlayerNotificationManager.Builder(
            context,
            NOW_PLAYING_NOTIFICATION_ID,
            NOW_PLAYING_CHANNEL_ID
        )
            .setChannelNameResourceId(R.string.notification_channel)
            .setChannelDescriptionResourceId(R.string.notification_channel_description)
            .setMediaDescriptionAdapter(DescriptionAdapter(mediaController))
            .build()
            .apply {
                setPlayer(player)
                setUsePlayPauseActions(true)
                setUseRewindAction(true)
                setUseRewindActionInCompactView(true)
                setColorized(true)
                setColor(Color.BLACK)

                setUseFastForwardAction(false)
                setUsePreviousAction(false)
                setUseChronometer(true)
            }

    }

    fun hideNotification() {
        notificationManager.setPlayer(null)
    }

    fun showNotificationForPlayer(player: Player) {
        notificationManager.setPlayer(player)
    }

    private inner class DescriptionAdapter(private val controller: ListenableFuture<MediaController>) :
        PlayerNotificationManager.MediaDescriptionAdapter {

        override fun createCurrentContentIntent(player: Player): PendingIntent? =
            controller.get().sessionActivity

        override fun getCurrentContentText(player: Player) = "Simply Awake"

        override fun getCurrentContentTitle(player: Player) =
            controller.get().mediaMetadata.title.toString()

        override fun getCurrentLargeIcon(
            player: Player,
            callback: PlayerNotificationManager.BitmapCallback
        ): Bitmap? {
            return /*(ContextCompat.getDrawable(context, R.drawable.enzo) as BitmapDrawable).bitmap*/ null
        }
    }
}