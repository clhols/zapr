package dk.youtec.zapr.model

import org.json.JSONArray
import org.json.JSONObject

class Program(val title: String, val programId: String, val startTime: Long, val endTime: Long,
              val shortDescription: String, val seriesTitle: String, val genreId: Int,
              val trickModes: Int, val image: String?, val eventId: Long) {

    companion object {
        fun fromJSONObject(jsonObject: JSONObject): Program {
            with(jsonObject) {
                val title = optString("title")
                val programId = optString("ProgramID")
                val startTime = optLong("starttime") * 1000
                val endTime = optLong("endtime") * 1000
                val shortDescription = optString("shortDescription")
                val seriesTitle = optString("seriesTitle")
                val genreId = if (!isNull("genre")) getJSONObject("genre").optInt("mainGenreId") else 0
                val trickModes = getInt("trickmodes")
                val image = if (!isNull("ProgramImages")) {
                    with(get("ProgramImages")) {
                        if (this is JSONArray)
                            getJSONObject(0).getString("ImageUrl")
                        else if (this is JSONObject)
                            getString("ImageUrl")
                        else null
                    }
                } else null
                val eventId = getLong("EventId")

                return Program(title, programId, startTime, endTime, shortDescription, seriesTitle, genreId, trickModes, image, eventId)
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Program

        if (title != other.title) return false
        if (programId != other.programId) return false
        if (startTime != other.startTime) return false
        if (endTime != other.endTime) return false
        if (shortDescription != other.shortDescription) return false
        if (seriesTitle != other.seriesTitle) return false
        if (genreId != other.genreId) return false
        if (trickModes != other.trickModes) return false
        if (image != other.image) return false
        if (eventId != other.eventId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + programId.hashCode()
        result = 31 * result + startTime.hashCode()
        result = 31 * result + endTime.hashCode()
        result = 31 * result + shortDescription.hashCode()
        result = 31 * result + seriesTitle.hashCode()
        result = 31 * result + (genreId.hashCode())
        result = 31 * result + trickModes
        result = 31 * result + (image?.hashCode() ?: 0)
        result = 31 * result + eventId.hashCode()
        return result
    }

    override fun toString(): String {
        return "Program(title='$title', programId='$programId', startTime=$startTime, endTime=$endTime, shortDescription='$shortDescription', seriesTitle='$seriesTitle', genreId=$genreId, trickModes=$trickModes, image=$image, eventId=$eventId)"
    }
}