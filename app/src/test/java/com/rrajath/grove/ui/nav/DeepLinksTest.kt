package com.rrajath.grove.ui.nav

import android.net.Uri
import androidx.navigation.NavDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Every deep link the app emits (bloom://) resolves to its NavHost destination,
 * and so does the same link under the legacy grove:// scheme, so shortcuts and
 * automations made before the rename keep working.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = android.app.Application::class, sdk = [34])
class DeepLinksTest {

    /** The deep-linked NavHost destinations, registered exactly as GroveApp.kt does. */
    private val destinations: Map<String, NavDestination> =
        listOf(Routes.NOTE, Routes.REMINDER, Routes.CAPTURE, Routes.CAPTURE_TEMPLATE, Routes.AGENDA)
            .associateWith { route ->
                NavDestination("test").apply { DeepLinks.navDeepLinks(route).forEach(::addDeepLink) }
            }

    private val emitted: List<Pair<String, String>> = listOf(
        DeepLinks.capture() to Routes.CAPTURE,
        DeepLinks.captureTemplate("builtin-journal") to Routes.CAPTURE_TEMPLATE,
        DeepLinks.agenda() to Routes.AGENDA,
        DeepLinks.note(Routes.encode("work/inbox.org#12"), mode = "read", isNew = false) to Routes.NOTE,
        DeepLinks.reminder("my notes.org", "Top/Sub heading", level = 2) to Routes.REMINDER,
    )

    private fun matching(uri: String): List<String> =
        destinations.filterValues { it.hasDeepLink(Uri.parse(uri)) }.keys.toList()

    @Test
    fun `emitted links use the bloom scheme and resolve to their destination`() {
        emitted.forEach { (uri, route) ->
            assertTrue(uri, uri.startsWith("bloom://"))
            assertEquals(uri, listOf(route), matching(uri))
        }
    }

    @Test
    fun `the same links under grove scheme still resolve`() {
        emitted.forEach { (uri, route) ->
            val legacy = "grove://" + uri.removePrefix("bloom://")
            assertEquals(legacy, listOf(route), matching(legacy))
        }
    }

    @Test
    fun `other schemes do not resolve`() {
        emitted.forEach { (uri, _) ->
            val other = "https://" + uri.removePrefix("bloom://")
            assertEquals(other, emptyList<String>(), matching(other))
        }
    }

    @Test
    fun `isAppScheme accepts both schemes only`() {
        assertTrue(DeepLinks.isAppScheme("bloom"))
        assertTrue(DeepLinks.isAppScheme("grove"))
        assertFalse(DeepLinks.isAppScheme("content"))
        assertFalse(DeepLinks.isAppScheme("file"))
        assertFalse(DeepLinks.isAppScheme(null))
    }

    @Test
    fun `link formats are unchanged apart from the scheme`() {
        // The same shapes the pre-rename code built by hand, so existing NavHost
        // argument parsing (and old pinned shortcuts) behave exactly as before.
        assertEquals("bloom://capture", DeepLinks.capture())
        assertEquals("bloom://capture/builtin-journal", DeepLinks.captureTemplate("builtin-journal"))
        assertEquals("bloom://agenda", DeepLinks.agenda())
        assertEquals("bloom://note/abc?mode=read&isNew=false", DeepLinks.note("abc", "read", false))
        assertEquals(
            "bloom://reminder/my%20notes.org?headingPath=Top%2FSub&level=2",
            DeepLinks.reminder("my notes.org", "Top/Sub", 2),
        )
    }

    @Test
    fun `no app-scheme URI is spelled out outside DeepLinks`() {
        val appDir = File("app").takeIf { File(it, "src/main").isDirectory } ?: File(".")
        val literal = Regex(""""[^"\n]*\b(grove|bloom)://""")
        val hits = File(appDir, "src/main/java").walkTopDown()
            .filter { it.isFile && it.extension == "kt" && it.name != "DeepLinks.kt" }
            .flatMap { file ->
                file.readLines().withIndex()
                    .filter { (_, line) -> line.trimStart().let { !it.startsWith("//") && !it.startsWith("/*") && !it.startsWith("*") } }
                    .filter { (_, line) -> literal.containsMatchIn(line) }
                    .map { (i, line) -> "${file.name}:${i + 1}: ${line.trim()}" }
            }.toList()
        assertTrue("Build deep links with DeepLinks instead:\n" + hits.joinToString("\n"), hits.isEmpty())
    }
}
