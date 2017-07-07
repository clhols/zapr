package dk.youtec.zapr.model

import android.support.v7.util.DiffUtil
import android.os.Bundle
import java.util.*

class EpgDiffCallback(val oldChannels: List<Channel>, val newChannels: List<Channel>) : DiffUtil.Callback() {

    override fun getOldListSize(): Int {
        return oldChannels.size
    }

    override fun getNewListSize(): Int {
        return newChannels.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldChannels[oldItemPosition].sid == newChannels[newItemPosition].sid
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldChannels[oldItemPosition] == newChannels[newItemPosition]
    }

    override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? {
        val newChannel = newChannels[newItemPosition]
        val oldChannel = oldChannels[oldItemPosition]

        if (newChannel.now != oldChannel.now) {
            return newChannel.now
        }
        if (Arrays.equals(newChannel.next, oldChannel.next)) {
            return newChannel.next
        }

        return super.getChangePayload(oldItemPosition, newItemPosition)
    }
}