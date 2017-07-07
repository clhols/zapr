package dk.youtec.zapr.ui

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.annotation.MainThread
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.AppCompatDelegate
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.text.InputType
import android.util.Log
import android.view.*
import android.widget.AdapterView
import android.widget.ProgressBar
import android.widget.Spinner
import dk.youtec.zapr.backend.*
import dk.youtec.zapr.R
import dk.youtec.zapr.backend.exception.BackendException
import dk.youtec.zapr.model.FavouriteList
import dk.youtec.zapr.ui.adapter.ChannelsAdapter
import dk.youtec.zapr.util.SharedPreferences
import dk.youtec.zapr.util.SharedPreferences.FAVOURITE_LIST_KEY
import org.jetbrains.anko.*
import dk.youtec.zapr.util.*
import java.text.SimpleDateFormat
import java.util.*
import android.widget.ArrayAdapter
import com.google.android.gms.gcm.GcmNetworkManager
import com.google.android.gms.gcm.OneoffTask
import com.google.android.gms.gcm.PeriodicTask
import dk.youtec.zapr.BuildConfig
import dk.youtec.zapr.model.Channel
import dk.youtec.zapr.model.Genre
import dk.youtec.zapr.service.UpcomingProgramsService

class MainActivity : AppCompatActivity(), AnkoLogger, ChannelsAdapter.OnChannelClickListener {
    private val TAG = MainActivity::class.java.simpleName

    private val LISTS_GROUP = 1

    private val mApi by lazy { BackendApi(this) }
    private val mRecyclerView by lazy { find<RecyclerView>(R.id.recycler_view) }
    private val mHandler = Handler()
    private val mEmptyState by lazy { find<ViewGroup>(R.id.empty_state) }
    private val mSwipeRefresh by lazy { find<SwipeRefreshLayout>(R.id.swipe_refresh) }
    private val mSpinner by lazy { find<Spinner>(R.id.spinner) }
    private val mGenreAdapter by lazy { ArrayAdapter<Genre>(this, R.layout.spinner_title) }
    private var mSelectedGenre: Genre? = null
    private val mProgressBar by lazy { find<ProgressBar>(R.id.progressBar) }
    private var mUpdateRunnable = Runnable {
        updateData()
    }
    private var mFavouriteLists: List<FavouriteList> = emptyList()
    private var mRemoteControl: Boolean = false

    companion object {
        init {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_AUTO)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)

        val toolbar = find<Toolbar>(R.id.toolbar)
        toolbar.title = ""
        setSupportActionBar(toolbar)

        if (forceRemoteControl()) {
            SharedPreferences.setBoolean(this, SharedPreferences.REMOTE_CONTROL_MODE, true)
        }
        mRemoteControl = SharedPreferences.getBoolean(this, SharedPreferences.REMOTE_CONTROL_MODE)

        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

        mSwipeRefresh.setOnRefreshListener {
            updateData()
        }

        //Genre filter spinner
        mSelectedGenre = Genre(0, getString(R.string.all))
        mGenreAdapter.setDropDownViewResource(R.layout.spinner_list_item)
        mGenreAdapter.setNotifyOnChange(false)
        mSpinner.adapter = mGenreAdapter
        mSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(adapterView: AdapterView<*>?) {
            }

            override fun onItemSelected(adapterView: AdapterView<*>, view: View?, position: Int, id: Long) {
                mSelectedGenre = adapterView.getItemAtPosition(position) as Genre
                mProgressBar.visibility = View.VISIBLE
                updateData() //TODO: Save a backing list for the ChannelsAdapter and filter the adapter instead of calling server
            }
        }

        //Schedule task
        /*
        val gcmNetworkManager = GcmNetworkManager.getInstance(this)
        gcmNetworkManager.cancelAllTasks(UpcomingProgramsService::class.java)

        val nowTask = OneoffTask.Builder()
                .setService(UpcomingProgramsService::class.java)
                .setExecutionWindow(0L, 10L)
                //.setRequiredNetwork(PeriodicTask.NETWORK_STATE_CONNECTED)
                .setTag(UpcomingProgramsService.UPCOMING_PROGRAMS)
                .build()
        gcmNetworkManager.schedule(nowTask)

        val task = PeriodicTask.Builder()
                .setService(UpcomingProgramsService::class.java)
                .setPeriod(24 * 60 * 60)
                //.setRequiredNetwork(PeriodicTask.NETWORK_STATE_CONNECTED)
                .setTag(UpcomingProgramsService.UPCOMING_PROGRAMS)
                .build()
        gcmNetworkManager.schedule(task)
        */
    }

    override fun onResume() {
        super.onResume()

        if (mRecyclerView.adapter != null) {
            updateData()
        } else {
            load()
        }
    }

    override fun onPause() {
        super.onPause()

        mHandler.removeCallbacks(mUpdateRunnable)
    }

    /**
     * Log in and retrieve channel data.
     */
    @MainThread
    private fun load() {
        mProgressBar.visibility = View.VISIBLE

        doAsync(exceptionHandler = { e -> error("Exception ${e.message}") }) {
            try {
                //Log in to get token
                val loggedIn = mApi.login()

                Log.d(TAG, "Logged in: ${loggedIn}")

                if (loggedIn) {
                    //Load smart cards and favourites
                    loadSmartCardsAndFavouriteLists()

                    //Load genres
                    loadGenres()

                    //Get EPG data
                    val epgResult = mApi.retrieveEpgData(mSelectedGenre!!.id)

                    //Set next updateData call to be triggered
                    if (epgResult.updateNextInSec > 0) {
                        mHandler.removeCallbacks(mUpdateRunnable)
                        mHandler.postDelayed(mUpdateRunnable, epgResult.updateNextInSec * 1000)
                    }

                    //Create adapter with data
                    uiThread {
                        setEmptyState(false)

                        mRecyclerView.adapter = ChannelsAdapter(this@MainActivity.contentView, epgResult.channels, this@MainActivity)
                    }
                } else {
                    uiThread { handleLoginFailure() }
                }
            } catch (e: BackendException) {
                Log.e(TAG, e.message, e)

                //If we got an exception, show it to the user
                uiThread { handleBackendException(e) }
            } finally {
                uiThread { mProgressBar.visibility = View.GONE }
            }
        }
    }

    @MainThread
    private fun handleBackendException(e: BackendException) {
        if (e.message != null) {
            toast(e.message)
        }

        setEmptyState(true)
    }

    @MainThread
    private fun handleLoginFailure() {
        if (SharedPreferences.getString(this@MainActivity, SharedPreferences.EMAIL).isBlank()) {
            showLoginDialog()
        } else {
            setEmptyState(true)

            toast(getString(R.string.loginError))
        }
    }

    @MainThread
    private fun loadSmartCardsAndFavouriteLists() {
        doAsync(exceptionHandler = { e -> error("Exception ${e.message}") }) {

            val id = mApi.retrieveSmartCards()
            val currentSmartCardId = SharedPreferences.getString(this@MainActivity, SharedPreferences.SMART_CARD_ID)

            if (id.isNotBlank() && id != currentSmartCardId) {
                //Save new smart card id
                SharedPreferences.setString(this@MainActivity, SharedPreferences.SMART_CARD_ID, id)

                //If we had a card already, note that we found a new one
                if (currentSmartCardId.isNotEmpty()) {
                    runOnUiThread { toast(getString(R.string.newTvBoxFound)) }
                }
            }

            if (id.isBlank()) {
                uiThread { toast(getString(R.string.noTvBoxError)) }
            }

            mFavouriteLists = mApi.retrieveFavouriteLists()
            uiThread { invalidateOptionsMenu() }
        }
    }

    /**
     * Show dialog to input login credentials.
     */
    @MainThread
    private fun showLoginDialog() {
        alert {
            title = getString(R.string.login_title)
            customView {
                verticalLayout {
                    padding = dip(24)

                    val email = editText {
                        hint = context.getString(R.string.email)
                    }.lparams(width = matchParent) {
                        bottomMargin = dip(12)
                    }
                    val password = editText {
                        hint = context.getString(R.string.password)
                        inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                    }
                    positiveButton(context.getString(R.string.login_button)) {
                        doAsync {
                            val status = mApi.login(email.text.toString(), password.text.toString())
                            if (status) {
                                SharedPreferences.setString(this@MainActivity, SharedPreferences.EMAIL, email.text.toString())
                                SharedPreferences.setString(this@MainActivity, SharedPreferences.PASSWORD, password.text.toString())

                                uiThread { load() }
                            } else {
                                uiThread {
                                    toast(getString(R.string.loginError))
                                    showLoginDialog()
                                }
                            }
                        }
                    }
                }
            }
        }.show()
    }

    /**
     * Refresh channels data
     */
    @MainThread
    private fun updateData() {
        doAsync {
            mHandler.removeCallbacks(mUpdateRunnable)

            if (LoginData.token.isEmpty()) {
                mApi.login()
            }

            try {
                Log.d(TAG, "Updating data")
                val epgResult = mApi.retrieveEpgData(mSelectedGenre!!.id)

                //Trigger next update delayed
                val time = if (epgResult.updateNextInSec > 0) epgResult.updateNextInSec * 1000 else 60000
                mHandler.postDelayed(mUpdateRunnable, time)

                //Log the time
                val localDateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                val nextTime = localDateFormat.format(Date(System.currentTimeMillis() + time))
                Log.d(TAG, "Next update at " + nextTime)

                uiThread {
                    mProgressBar.visibility = View.GONE
                    mSwipeRefresh.isRefreshing = false
                    (mRecyclerView.adapter as? ChannelsAdapter)?.updateList(epgResult.channels)
                }
            } catch (e: BackendException) {
                uiThread {
                    mSwipeRefresh.isRefreshing = false
                    load()
                }
            }
        }
    }

    fun setEmptyState(show: Boolean) {
        if (show) {
            mEmptyState.visibility = View.VISIBLE
            mRecyclerView.visibility = View.GONE
        } else {
            mEmptyState.visibility = View.GONE
            mRecyclerView.visibility = View.VISIBLE
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)

        val listsMenu = menu.findItem(R.id.menu_favourite_lists)
        val subMenu = listsMenu.subMenu

        for ((key, name) in mFavouriteLists) {
            subMenu.add(LISTS_GROUP, key, Menu.NONE, name)
        }
        listsMenu.isVisible = mFavouriteLists.isNotEmpty()

        val remoteControlMenuItem = menu.findItem(R.id.menu_remote_control)
        remoteControlMenuItem.isVisible = !forceRemoteControl()
        colorMenuItem(remoteControlMenuItem, if (mRemoteControl) resources.getColor(R.color.colorAccent) else resources.getColor(android.R.color.white), null)
        return true
    }

    private fun forceRemoteControl() = false

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.groupId == LISTS_GROUP) {
            val id = item.itemId
            SharedPreferences.setString(applicationContext, FAVOURITE_LIST_KEY, id.toString())
            updateData()
        }

        if (item.itemId == R.id.menu_remote_control) {
            SharedPreferences.setBoolean(this, SharedPreferences.REMOTE_CONTROL_MODE, !mRemoteControl)
            mRemoteControl = SharedPreferences.getBoolean(this, SharedPreferences.REMOTE_CONTROL_MODE)

            colorMenuItem(item, if (mRemoteControl) resources.getColor(R.color.colorAccent) else resources.getColor(android.R.color.white), null)
        }

        return super.onOptionsItemSelected(item)
    }

    fun loadGenres() {
        doAsync {
            val genres = mApi.retrieveEpgGenres()

            uiThread {
                mGenreAdapter.clear()
                mGenreAdapter.add(Genre(0, getString(R.string.all)))
                mGenreAdapter.addAll(genres)
                mGenreAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun changeToChannel(channel: Channel, restart: Boolean) {
        doAsync {
            val eventId = if (restart && channel.now != null) channel.now.eventId else 0

            val success = mApi.changeToChannel(channel.sid, eventId, restart)

            if (!success) {
                uiThread { toast(getString(R.string.cantChangeChannel)) }
            }
        }
    }

    override fun playChannel(channel: Channel, restart: Boolean) {
        doAsync {
            val eventId = if (restart && channel.now != null) channel.now.eventId else 0

            try {
                val uri = mApi.getStreamUrl(channel.sid, eventId, restart)

                if (uri.isNotBlank()) {
                    val intent = buildIntent(this@MainActivity, uri)
                    this@MainActivity.startActivity(intent)
                } else {
                    uiThread { toast(getString(R.string.cantChangeChannel)) }
                }
            } catch (e: BackendException) {
                Log.e(TAG, e.message, e)
                if (e.code == ERROR_MUST_BE_LOGGED_IN && mApi.login()) {
                    playChannel(channel, restart)
                } else {
                    uiThread { toast(if (e.message != null && e.message != "Success") e.message else getString(R.string.cantChangeChannel)) }
                }
            }
        }
    }

    fun buildIntent(context: Context, uri: String): Intent {
        val preferExtensionDecoders = false

        val intent = Intent(context, PlayerActivity::class.java)
        with(intent) {
            action = PlayerActivity.ACTION_VIEW
            putExtra(PlayerActivity.PREFER_EXTENSION_DECODERS, preferExtensionDecoders)
            setData(Uri.parse(uri))
        }

        return intent
    }

    override fun showChannel(context: Context, channel: Channel) {
        val intent = Intent(context, ProgramsActivity::class.java)
        with(intent) {
            putExtra(ProgramsActivity.CHANNEL_NAME, channel.channelName)
            putExtra(ProgramsActivity.CHANNEL_SID, channel.sid)
        }


        if (Build.VERSION.SDK_INT > 15) {
            val translateBundle = ActivityOptions.makeCustomAnimation(context,
                    R.anim.slide_in_left, R.anim.slide_out_left).toBundle()

            context.startActivity(intent, translateBundle)
        } else {
            context.startActivity(intent)
        }
    }
}
