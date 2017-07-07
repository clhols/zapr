package dk.youtec.zapr.ui

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import dk.youtec.zapr.R
import dk.youtec.zapr.backend.BackendApi
import dk.youtec.zapr.ui.adapter.ProgramAdapter
import dk.youtec.zapr.util.SharedPreferences
import org.jetbrains.anko.displayMetrics
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.find
import org.jetbrains.anko.uiThread

class ProgramsActivity : AppCompatActivity() {
    private val mApi by lazy { BackendApi(this) }
    private val mRecyclerView by lazy { find<RecyclerView>(R.id.recycler_view) }
    private val mToolbarTitle by lazy { find<TextView>(R.id.toolbar_title) }
    private val mProgressBar by lazy { find<ProgressBar>(R.id.progressBar) }
    private var mRemoteControl: Boolean = false

    companion object {
        val CHANNEL_NAME = "extra_channel_name"
        val CHANNEL_SID = "extra_channel_sid"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_programs)

        //Setup toolbar
        val toolbar = find<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        mToolbarTitle.text = intent.extras.get(CHANNEL_NAME) as String
        setSupportActionBar(toolbar)

        //Setup toolbar up navigation
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp)
        toolbar.setNavigationOnClickListener { onBackPressed() }

        mRemoteControl = SharedPreferences.getBoolean(this, SharedPreferences.REMOTE_CONTROL_MODE)

        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        loadPrograms()
    }

    fun loadPrograms() {
        mProgressBar.visibility = View.VISIBLE
        doAsync {
            val sid = intent.extras.get(CHANNEL_SID) as String
            val programs = mApi.retrievePrograms(sid)

            val currentIndex = programs.indexOfFirst {
                val time = System.currentTimeMillis()
                it.startTime <= time && it.endTime >= time
            }

            uiThread {
                mProgressBar.visibility = View.GONE

                mRecyclerView.adapter = ProgramAdapter(this@ProgramsActivity, sid, programs, mApi)
                (mRecyclerView.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(currentIndex, displayMetrics.heightPixels / 6)
            }
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_right)
    }
}
