package dk.youtec.zapr.model

import org.json.JSONObject
import java.util.*

data class Genre(val id: Int, val name: String, val subGenres: List<Genre>? = null) {
    companion object {
        fun fromJSONObject(id: Int, jsonObject: JSONObject): Genre = Genre(id, jsonObject.getString("genreName"), getSubGenres(jsonObject.optJSONObject("subGenres")))

        fun getSubGenres(data: JSONObject?): List<Genre> {
            val subGenres: ArrayList<Genre> = ArrayList()
            if(data != null) {
                for (id in data.keys()) {
                    val name = data.optString(id)

                    subGenres.add(Genre(id.toInt(), name, null))
                }
            }

            return subGenres
        }
    }

    override fun toString(): String {
        return name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as Genre

        if (id != other.id) return false
        if (name != other.name) return false
        if (subGenres != other.subGenres) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id
        result = 31 * result + name.hashCode()
        if(subGenres != null) {
            result = 31 * result + subGenres.hashCode()
        }
        return result
    }
}