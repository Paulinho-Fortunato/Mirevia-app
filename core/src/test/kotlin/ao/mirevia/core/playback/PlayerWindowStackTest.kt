package ao.mirevia.core.playback

import org.junit.Assert.*
import org.junit.Test

class PlayerWindowStackTest {
    @Test
    fun `window count is bounded and closing one restores capacity`() {
        val stack = PlayerWindowStack<String>()
        val owner = Any()
        val entries =
            (1..PlayerWindowStack.MAX_WINDOWS).map { stack.add(owner, null, "window-$it")!! }
        assertNull(stack.add(owner, null, "too many"))
        assertEquals(3, stack.entries.size)
        stack.remove(entries.last().id)
        assertNotNull(stack.add(owner, null, "new window"))
        assertEquals(3, stack.entries.size)
    }

    @Test
    fun `only an existing opener from the same owner can create a child`() {
        data class Owner(val label: String)
        val stack = PlayerWindowStack<String>()
        val owner = Owner("same label")
        val parent = stack.add(owner, null, "parent")!!
        assertNull(stack.add(Owner("same label"), parent.id, "not this session"))
        assertNull(stack.add(owner, Long.MAX_VALUE, "unknown opener"))
        assertNotNull(stack.add(owner, parent.id, "child"))
        assertEquals(2, stack.entries.size)
    }

    @Test
    fun `closing a child destroys its descendants first but leaves its opener`() {
        val stack = PlayerWindowStack<String>()
        val owner = Any()
        val parent = stack.add(owner, null, "parent")!!
        val child = stack.add(owner, parent.id, "child")!!
        stack.add(owner, child.id, "grandchild")!!
        assertEquals(listOf("grandchild", "child"), stack.remove(child.id).map { it.value })
        assertEquals(listOf("parent"), stack.entries.map { it.value })
    }

    @Test
    fun `closing a window does not destroy an unrelated sibling`() {
        val stack = PlayerWindowStack<String>()
        val owner = Any()
        val parent = stack.add(owner, null, "parent")!!
        val child = stack.add(owner, parent.id, "child")!!
        stack.add(owner, null, "independent")!!
        assertEquals(listOf("child"), stack.remove(child.id).map { it.value })
        assertEquals(listOf("parent", "independent"), stack.entries.map { it.value })
    }

    @Test
    fun `releasing an owner cannot close a different session`() {
        val stack = PlayerWindowStack<String>()
        val first = Any()
        val second = Any()
        val root = stack.add(first, null, "first")!!
        stack.add(first, root.id, "first-child")!!
        stack.add(second, null, "second")!!
        assertEquals(listOf("first-child", "first"), stack.removeOwner(first).map { it.value })
        assertEquals(listOf("second"), stack.entries.map { it.value })
        assertTrue(stack.removeOwner(first).isEmpty())
    }

    @Test
    fun `clearing is child-first idempotent and does not reuse stale identifiers`() {
        val stack = PlayerWindowStack<String>()
        val owner = Any()
        val first = stack.add(owner, null, "first")!!
        val second = stack.add(owner, first.id, "second")!!
        assertEquals(listOf(second.id, first.id), stack.clear().map { it.id })
        assertTrue(stack.clear().isEmpty())
        val fresh = stack.add(owner, null, "fresh")!!
        assertTrue(fresh.id > second.id)
        assertTrue(stack.remove(first.id).isEmpty())
        assertEquals(listOf(fresh), stack.entries)
    }

    @Test
    fun `reading a snapshot does not expose the mutable backing list`() {
        val stack = PlayerWindowStack<String>()
        val owner = Any()
        stack.add(owner, null, "first")
        val snapshot = stack.entries
        stack.add(owner, null, "second")
        assertEquals(listOf("first"), snapshot.map { it.value })
        assertEquals(2, stack.entries.size)
    }

    @Test
    fun `an unknown window cannot close another window or create an opener`() {
        val stack = PlayerWindowStack<String>()
        val owner = Any()
        stack.add(owner, null, "first")
        assertTrue(stack.remove(-1).isEmpty())
        assertNull(stack.add(owner, -1, "unknown child"))
        assertEquals(listOf("first"), stack.entries.map { it.value })
    }
}
