package ao.mirevia.core.playback

/** UI-thread-owned bounded window graph. Values may be native handles; no URL or network logic. */
class PlayerWindowStack<T : Any> {
    data class Entry<T>(val id: Long, val owner: Any, val openerId: Long?, val value: T)

    private val records = mutableListOf<Entry<T>>()
    private var nextId = 0L
    val entries: List<Entry<T>>
        get() = records.toList()

    fun add(owner: Any, openerId: Long?, value: T): Entry<T>? {
        if (records.size >= MAX_WINDOWS) return null
        if (openerId != null && records.none { it.id == openerId && it.owner === owner })
            return null
        return Entry(++nextId, owner, openerId, value).also(records::add)
    }

    /** Destroy descendants before their opener, but never unrelated windows or the iframe owner. */
    fun remove(id: Long): List<Entry<T>> {
        if (records.none { it.id == id }) return emptyList()
        val ids = mutableSetOf(id)
        for (entry in records) if (entry.openerId in ids) ids += entry.id
        return removeWhere { it.id in ids }
    }

    fun removeOwner(owner: Any): List<Entry<T>> = removeWhere { it.owner === owner }

    fun clear(): List<Entry<T>> = removeWhere { true }

    private fun removeWhere(predicate: (Entry<T>) -> Boolean): List<Entry<T>> {
        val removed = records.filter(predicate)
        records.removeAll(removed.toSet())
        return removed.asReversed()
    }

    companion object {
        const val MAX_WINDOWS = 3
    }
}
