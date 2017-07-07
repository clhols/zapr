package dk.youtec.zapr.ui.adapter

import android.content.Context
import android.support.annotation.LayoutRes
import android.support.annotation.MainThread
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import dk.youtec.zapr.backend.*
import dk.youtec.zapr.model.Channel
import dk.youtec.zapr.R
import dk.youtec.zapr.util.SharedPreferences
import org.jetbrains.anko.*
import java.text.SimpleDateFormat
import java.util.*
import android.support.v7.util.DiffUtil
import android.support.v7.util.ListUpdateCallback
import android.util.Log
import android.widget.ImageButton
import android.widget.ProgressBar
import dk.youtec.zapr.model.EpgDiffCallback
import dk.youtec.zapr.ui.view.AspectImageView

fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View {
    return LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
}

class ChannelsAdapter(val contentView: View?, var channels: List<Channel>, val listener: OnChannelClickListener) : RecyclerView.Adapter<ChannelsAdapter.ViewHolder>() {

    //Toggles if description and image should be shown
    var showDetails = true

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val channel = channels[position]
        holder.mChannelName.text = channel.channelName
        if (channel.now != null) {
            holder.mChannelName.visibility = View.GONE
            holder.mTitle.text = channel.now.title
            holder.mNowDescription.text = channel.now.shortDescription
            holder.mNowDescription.visibility = if (showDetails) View.VISIBLE else View.GONE

            //Show time interval
            val localDateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
            val startTime = localDateFormat.format(Date(channel.now.startTime))
            val endTime = localDateFormat.format(Date(channel.now.endTime))

            holder.mTime.text = buildString {
                append(startTime)
                append(" - ")
                append(endTime)
            }

            //Progress
            val programDuration = channel.now.endTime - channel.now.startTime
            val programTime = System.currentTimeMillis() - channel.now.startTime
            val percentage = 100 * programTime.toFloat() / programDuration
            holder.mProgress.progress = percentage.toInt()

            //Genre icon
            holder.mGenre.setImageResource(0)
            if (channel.now.genreId == 10) {
                //Sport genre
                holder.mGenre.setImageResource(R.drawable.ic_genre_dot_black);
                holder.mGenre.setColorFilter(holder.itemView.resources.getColor(android.R.color.holo_blue_dark))
            }

            //holder.mTimeLeft.text = "(" + (channel.now.endTime - System.currentTimeMillis()) / 60 + " min left)"

            if (!channel.now.image.isNullOrEmpty() && showDetails) {
                holder.mImage.visibility = View.VISIBLE
                Glide.with(holder.mImage.context)
                        .load(channel.now.image)
                        .placeholder(R.drawable.image_placeholder)
                        .into(holder.mImage)
            } else {
                holder.mImage.visibility = View.GONE
                holder.mImage.image = null
            }

            Glide.with(holder.mLogo.context)
                    .load(URL_GET_GFX.replace("[SID]", channel.sid, true).replace("[SIZE]", 60.toString()))
                    .override(Target.SIZE_ORIGINAL, Target.SIZE_ORIGINAL)
                    .into(holder.mLogo)
        } else {
            holder.mChannelName.visibility = View.VISIBLE
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.channels_item))
    }

    @MainThread
    fun updateList(newList: List<Channel>) {
        //Calculate the diff
        val diffResult = DiffUtil.calculateDiff(EpgDiffCallback(channels, newList))

        //Update the backing list
        channels = newList

        diffResult.dispatchUpdatesTo(object : ListUpdateCallback {
            override fun onInserted(position: Int, count: Int) {
            }

            override fun onRemoved(position: Int, count: Int) {
            }

            override fun onMoved(fromPosition: Int, toPosition: Int) {
            }

            override fun onChanged(position: Int, count: Int, payload: Any) {
                if (position < newList.size) {
                    val (sid, channelName, now, next) = newList[position]

                    if (now != null && contentView != null) {
                        val message = now.title + " " + contentView.context.getString(R.string.on) + " " + channelName
                        Log.d(TAG, message)

                        //contentView.context.longToast(message)
                    }
                }
            }
        })

        //Make the adapter notify of the changes
        diffResult.dispatchUpdatesTo(this)
    }

    override fun getItemCount(): Int {
        return channels.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val TAG = ViewHolder::class.java.simpleName
        var mChannelName: TextView
        var mTitle: TextView
        var mProgress: ProgressBar
        var mNowDescription: TextView
        var mImage: ImageView
        var mLogo: ImageView
        var mTime: TextView
        var mGenre: ImageView
        var mMore: ImageButton

        init {
            mChannelName = itemView.find<TextView>(R.id.channelName)
            itemView.setOnClickListener {
                if (0 <= adapterPosition && adapterPosition < channels.size) {
                    if (SharedPreferences.getBoolean(it.context, SharedPreferences.REMOTE_CONTROL_MODE)) {
                        listener.changeToChannel(channels[adapterPosition])
                    } else {
                        listener.playChannel(channels[adapterPosition])
                    }
                }
            }
            itemView.setOnLongClickListener {
                it.context.selector(it.context.getString(R.string.channelAction), listOf(it.context.getString(R.string.startOverAction))) { _, _ ->
                    if (0 <= adapterPosition && adapterPosition < channels.size) {
                        if (SharedPreferences.getBoolean(it.context, SharedPreferences.REMOTE_CONTROL_MODE)) {
                            listener.changeToChannel(channels[adapterPosition], true)
                        } else {
                            listener.playChannel(channels[adapterPosition], true)
                        }
                    }
                }
                true
            }
            mTitle = itemView.findViewById(R.id.title)
            mProgress = itemView.findViewById(R.id.progress)
            mNowDescription = itemView.findViewById(R.id.nowDescription)
            mImage = itemView.findViewById(R.id.image)
            mTime = itemView.findViewById(R.id.time)
            mLogo = itemView.findViewById(R.id.logo)
            mGenre = itemView.findViewById(R.id.genre)
            mMore = itemView.findViewById(R.id.more)
            mMore.setOnClickListener {
                listener.showChannel(it.context, channels[adapterPosition])
            }

            (mImage as AspectImageView).setAspectRatio(292, 189)
        }
    }

    interface OnChannelClickListener {
        fun showChannel(context: Context, channel: Channel)
        fun playChannel(channel: Channel, restart: Boolean = false)
        fun changeToChannel(channel: Channel, restart: Boolean = false)
    }
}