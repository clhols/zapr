package dk.youtec.zapr.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.android.gms.gcm.GcmTaskService
import android.util.Log
import com.google.android.gms.gcm.GcmNetworkManager
import com.google.android.gms.gcm.TaskParams
import dk.youtec.zapr.backend.BackendApi
import dk.youtec.zapr.backend.exception.BackendException
import dk.youtec.zapr.receiver.UpcomingProgramReceiver
import java.text.SimpleDateFormat
import java.util.*

class UpcomingProgramsService : GcmTaskService() {
    private val TAG = UpcomingProgramsService::class.java.simpleName

    companion object {
        val UPCOMING_PROGRAMS = "upcoming_programs_task"
    }

    override fun onInitializeTasks() {
        // When your package is removed or updated, all of its network tasks are cleared by
        // the GcmNetworkManager. You can override this method to reschedule them in the case of
        // an updated package. This is not called when your application is first installed.
        //
        // This is called on your application's main thread.

        // TODO(developer): In a real app, this should be implemented to re-schedule important tasks.
    }

    override fun onRunTask(taskParams: TaskParams): Int {
        Log.d(TAG, "onRunTask: " + taskParams.tag)

        val tag = taskParams.tag

        // Default result is success.
        var result = GcmNetworkManager.RESULT_SUCCESS

        //TODO: Set alarms for upcoming programs, so the user can be notified.
        val api = BackendApi(applicationContext)

        try {
            val (updateNextInSec, channels) = api.retrieveEpgData()

            channels.forEach {
                val programs = api.retrievePrograms(it.sid, System.currentTimeMillis(), 25)
                programs.forEach {
                    if (it.genreId == 10) { //Sport
                        val time = it.startTime - 15 * 60 * 1000

                        val intent = Intent(this, UpcomingProgramReceiver::class.java)
                        intent.action = "dk.youtec.zapr.UPCOMING_PROGRAM"
                        intent.data = Uri.parse("content:" + it.programId)
                        intent.putExtra("title", it.title)
                        intent.putExtra("shortDescription", it.shortDescription)
                        intent.putExtra("startTime", it.startTime)
                        intent.putExtra("eventId", it.eventId)
                        intent.putExtra("programId", it.programId)
                        intent.putExtra("image", it.image)
                        val pendingIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

                        val localDateFormat = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
                        Log.d(TAG, "Scheduling alarm at " + localDateFormat.format(Date(time)) + " for " + it.title)

                        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).set(AlarmManager.RTC_WAKEUP, time, pendingIntent)
                    }
                }
            }

        } catch (e: BackendException) {
            return GcmNetworkManager.RESULT_FAILURE
        }

        // Return one of RESULT_SUCCESS, RESULT_FAILURE, or RESULT_RESCHEDULE
        return result
    }
}