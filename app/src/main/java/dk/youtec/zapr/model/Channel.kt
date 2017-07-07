package dk.youtec.zapr.model

import org.json.JSONObject
import java.util.*

data class Channel(val sid: String, val channelName: String, val now: Program?, val next: Array<Program>) {
    companion object {
        fun fromJSONObject(channel: JSONObject): Channel =
                Channel(channel.optString("sid"),
                        channel.optString("channelName"),
                        if (channel.isNull("now")) null else Program.fromJSONObject(channel.getJSONObject("now")),
                        if (channel.isNull("next")) arrayOf() else Array(channel.getJSONArray("next").length(), { i -> Program.fromJSONObject(channel.getJSONArray("next").getJSONObject(i)) }))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Channel

        if (sid != other.sid) return false
        if (channelName != other.channelName) return false
        if (now != other.now) return false
        if (!Arrays.equals(next, other.next)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sid.hashCode()
        result = 31 * result + channelName.hashCode()
        result = 31 * result + (now?.hashCode() ?: 0)
        result = 31 * result + Arrays.hashCode(next)
        return result
    }
}