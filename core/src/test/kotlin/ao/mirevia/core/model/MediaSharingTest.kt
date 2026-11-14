package ao.mirevia.core.model

import ao.mirevia.core.playback.*
import org.junit.Assert.*
import org.junit.Test

class MediaSharingTest {
    private val policy = EmbedUrlPolicy(setOf("myembed.biz"))
    private val movie =
        MediaSummary(MediaKey(DataOrigin.TMDB, MediaType.MOVIE, 969681), "Título de teste")
    private val series =
        MediaSummary(MediaKey(DataOrigin.TMDB, MediaType.TV, 30984), "Série de teste")

    private fun source(url: String) =
        VideoSource(url, VideoSourceType.IFRAME, "EmbedMovies", true, true, true, false)

    @Test
    fun `title sharing uses the official metadata page not a playback promise`() {
        assertEquals(
            "Título de teste\nhttps://www.themoviedb.org/movie/969681",
            MediaSharing.title(movie)?.text,
        )
        assertTrue(
            MediaSharing.title(series)!!.text.endsWith("https://www.themoviedb.org/tv/30984")
        )
    }

    @Test
    fun `shares the documented movie and episode iframe links with an honest notice`() {
        listOf("https://myembed.biz/filme/969681", "https://myembed.biz/filme/tt22084616")
            .forEach { url ->
                val payload = MediaSharing.embed(movie, source(url), policy)!!
                assertTrue(payload.text.contains(url))
                assertTrue(payload.text.contains("exclusivamente via iframe"))
                assertTrue(payload.text.contains("não é um link directo de vídeo"))
            }
        val payload =
            MediaSharing.embed(series, source("https://myembed.biz/serie/30984/1/1"), policy)!!
        assertTrue(payload.subject.contains("Temporada 1 · Episódio 1"))
        assertTrue(payload.text.contains("https://myembed.biz/serie/30984/1/1"))
    }

    @Test
    fun `a series selection URL can be shared without inventing an episode`() {
        val payload =
            MediaSharing.embed(series, source("https://myembed.biz/serie/30984"), policy)!!
        assertFalse(payload.subject.contains("Episódio"))
    }

    @Test
    fun `demo entries do not claim real metadata or embed links`() {
        val demo = movie.copy(key = movie.key.copy(origin = DataOrigin.DEMO))
        assertNull(MediaSharing.title(demo))
        assertNull(MediaSharing.embed(demo, source("https://myembed.biz/filme/969681"), policy))
    }

    @Test
    fun `unvalidated sources and query parameters are never shared`() {
        listOf(
                "https://evil.example/filme/969681",
                "http://myembed.biz/filme/969681",
                "https://myembed.biz/filme/969681?token=secret",
                "https://myembed.biz/filme/969681#secret",
                "https://myembed.biz/video.mp4",
                "https://myembed.biz/serie/30984/0/1",
            )
            .forEach { assertNull(it, MediaSharing.embed(movie, source(it), policy)) }
        assertNull(MediaSharing.embed(movie, null, policy))
        assertNull(
            MediaSharing.embed(
                movie,
                source("https://myembed.biz/filme/969681").copy(isIframe = false),
                policy,
            )
        )
    }

    @Test
    fun `title type and numeric id must match the route`() {
        assertNull(MediaSharing.embed(movie, source("https://myembed.biz/serie/30984"), policy))
        assertNull(MediaSharing.embed(series, source("https://myembed.biz/filme/969681"), policy))
        assertNull(
            MediaSharing.embed(series, source("https://myembed.biz/serie/30985/1/1"), policy)
        )
        assertNull(MediaSharing.embed(movie, source("https://myembed.biz/filme/1"), policy))
    }

    @Test
    fun `share subject cannot inject control characters or unbounded text`() {
        val payload = MediaSharing.title(movie.copy(title = "A\n\r\t" + "x".repeat(300)))!!
        assertTrue(payload.subject.length <= 200)
        assertFalse(payload.subject.any(Char::isISOControl))
        assertEquals("Título TMDB", MediaSharing.title(movie.copy(title = "\n"))!!.subject)
    }
}
