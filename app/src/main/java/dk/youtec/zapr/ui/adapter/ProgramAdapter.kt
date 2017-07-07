package dk.youtec.zapr.ui.adapter

import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.net.Uri
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import dk.youtec.zapr.backend.*
import dk.youtec.zapr.R
import dk.youtec.zapr.util.SharedPreferences
import org.jetbrains.anko.*
import java.text.SimpleDateFormat
import java.util.*
import dk.youtec.zapr.model.Program
import dk.youtec.zapr.ui.PlayerActivity
import dk.youtec.zapr.ui.view.AspectImageView

class ProgramAdapter(val context: Context, val channelSID: String, val programs: List<Program>, val api: BackendApi) : RecyclerView.Adapter<ProgramAdapter.ViewHolder>() {

    private val MASK_STARTOVER_FAST_FORWARD = 5
    private var mColorMatrixColorFilter: ColorMatrixColorFilter
    private var mResources: Resources

    init {
        val matrix = ColorMatrix()
        matrix.setSaturation(0f)
        mColorMatrixColorFilter = ColorMatrixColorFilter(matrix)
        mResources = context.resources
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val program = programs[position]
        holder.mEnabled = program.trickModes and MASK_STARTOVER_FAST_FORWARD != 0

        //Title and description
        holder.mTitle.text = program.title
        holder.mNowDescription.text = program.shortDescription

        //Time
        val localDateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
        val startDate = localDateFormat.format(Date(program.startTime))

        val localTimeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val startTime = localTimeFormat.format(Date(program.startTime))
        val endTime = localTimeFormat.format(Date(program.endTime))

        holder.mTime.text = buildString {
            append(startDate)
            append(" ")
            append(startTime)
            append(" - ")
            append(endTime)
        }

        //Header color
        if(program.startTime < System.currentTimeMillis() && System.currentTimeMillis() <= program.endTime) {
            holder.mLive.visibility = View.VISIBLE
            //holder.mHeader.setBackgroundColor(mResources.getColor(R.color.liveProgramHeaderBackground))
        } else {
            holder.mLive.visibility = View.GONE
            //holder.mHeader.setBackgroundColor(mResources.getColor(R.color.channelHeaderBackground))
        }

        //Genre icon
        if (holder.mEnabled) {
            holder.mGenre.setImageResource(0)
            if (program.genreId == 10) {
                //Sport genre
                holder.mGenre.setImageResource(R.drawable.ic_genre_dot_black);
                if (holder.mEnabled) {
                    holder.mGenre.setColorFilter(mResources.getColor(android.R.color.holo_blue_dark))
                } else {
                    holder.mGenre.colorFilter = mColorMatrixColorFilter
                }
            }
        } else {
            holder.mGenre.setImageResource(0)
        }

        //Image
        if (!program.image.isNullOrEmpty()) {
            holder.mImage.visibility = View.VISIBLE
            Glide.with(holder.mImage.context)
                    .load(program.image)
                    .placeholder(R.drawable.image_placeholder)
                    .into(holder.mImage)
        } else {
            holder.mImage.visibility = View.GONE
            holder.mImage.image = null
        }

        //Set view enabled state
        holder.itemView.isClickable = holder.mEnabled
        holder.itemView.isEnabled = holder.mEnabled
        holder.mTitle.isEnabled = holder.mEnabled
        holder.mNowDescription.isEnabled = holder.mEnabled
        holder.mTime.isEnabled = holder.mEnabled
        holder.mImage.colorFilter = if (holder.mEnabled) null else mColorMatrixColorFilter
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(parent.inflate(R.layout.program_item))
    }

    override fun getItemCount(): Int {
        return programs.size
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val TAG = ViewHolder::class.java.simpleName
        var mHeader: View
        var mTitle: TextView
        var mNowDescription: TextView
        var mImage: ImageView
        var mTime: TextView
        var mGenre: ImageView
        var mLive: TextView
        var mEnabled: Boolean = false

        init {
            itemView.setOnClickListener {
                handleClick(it)
            }
            mHeader = itemView.findViewById(R.id.programHeader)
            mTitle = itemView.findViewById(R.id.title)
            mNowDescription = itemView.findViewById(R.id.nowDescription)
            mImage = itemView.findViewById(R.id.image)
            mTime = itemView.find<TextView>(R.id.time)
            mGenre = itemView.find<ImageView>(R.id.genre)
            mLive = itemView.find<TextView>(R.id.live)

            (mImage as AspectImageView).setAspectRatio(292, 189)
        }

        private fun handleClick(it: View) {
            val program = programs[adapterPosition]
            if (program.startTime < System.currentTimeMillis()) {
                if (SharedPreferences.getBoolean(it.context, SharedPreferences.REMOTE_CONTROL_MODE)) {
                    changeToChannel(program)
                } else {
                    playChannel(program)
                }
            } else {
                it.context.toast(it.context.getString(R.string.upcomingTransmission))
            }
        }

        private fun changeToChannel(program: Program) {
            doAsync {
                val success = api.changeToChannel(channelSID, program.eventId, true)

                if (!success) {
                    uiThread { itemView.context.toast(itemView.context.getString(R.string.cantChangeChannel)) }
                }
            }
        }

        private fun playChannel(program: Program) {
            doAsync {
                val uri = api.getStreamUrl(channelSID, program.eventId)

                if (uri.isNotBlank()) {
                    val intent = buildIntent(context, uri)
                    context.startActivity(intent)
                } else {
                    uiThread { context.toast(context.applicationContext.getString(R.string.cantChangeChannel)) }
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

}