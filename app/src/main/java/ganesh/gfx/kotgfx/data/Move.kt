package ganesh.gfx.kotgfx.data

import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class Move(val userId: String? = null, val move: String? = null) {
    // Null default values create a no-argument default constructor, which is needed
    // for deserialization from a DataSnapshot.
    override fun toString(): String {
        return "Move(userId=$userId, move=$move)"
    }
}
