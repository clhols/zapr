package dk.youtec.zapr.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.app.NotificationCompat
import android.util.Log

class UpcomingProgramReceiver : BroadcastReceiver() {
    private val TAG = UpcomingProgramReceiver::class.java.simpleName

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Notify user of program " + intent.getStringExtra("title"))

        val builder = NotificationCompat.Builder(context)
                .setWhen(intent.getLongExtra("startTime", 0))
                .setContentTitle(intent.getStringExtra("title"))
                .setAutoCancel(true)
                .setStyle(NotificationCompat.BigTextStyle()
                    .bigText(intent.getStringExtra("shortDescription")))

        val notification = builder.build()

        //notificationManager.notify(intent., notification)
    }
}