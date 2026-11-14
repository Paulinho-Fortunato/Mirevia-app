package ao.mirevia.data

import ao.mirevia.core.model.*
import ao.mirevia.data.remote.tmdb.*
import org.junit.Assert.*
import org.junit.Test

class TmdbMappingTest {
    private val images =
        TmdbImages(
            ImageConfigurationDto(
                "https://image.tmdb.org/t/p/",
                listOf("w342"),
                listOf("w780"),
                listOf("w185"),
                listOf("w300"),
            )
        )

    @Test
    fun `missing values stay absent instead of invented`() {
        val details = MediaDetailsDto(id = 11, title = "Fixture").toDetails(MediaType.MOVIE, images)
        assertNull(details.media.rating)
        assertNull(details.media.releaseDate)
        assertNull(details.runtimeMinutes)
        assertNull(details.imdbId)
        assertTrue(details.seasons.isEmpty())
    }

    @Test
    fun `series name and first air date map correctly`() {
        val media =
            MediaDto(id = 1, name = "Fixture TV", firstAirDate = "2024-03-01")
                .toSummary(images, MediaType.TV)!!
        assertEquals("Fixture TV", media.title)
        assertEquals(2024, media.year)
        assertEquals(MediaType.TV, media.key.type)
    }

    @Test
    fun `adult and person cannot be mapped to playable media`() {
        assertNull(MediaDto(id = 1, adult = true).toSummary(images, MediaType.MOVIE))
        assertNull(MediaDto(id = 1, mediaType = "person").toSummary(images, MediaType.MOVIE))
    }

    @Test
    fun `trailer must be official supported site and valid key`() {
        assertNull(
            VideoDto(key = "abcdefghijk", site = "YouTube", type = "Trailer", official = false)
                .toOfficialTrailer()
        )
        assertNull(
            VideoDto(key = "javascript:bad", site = "YouTube", type = "Trailer", official = true)
                .toOfficialTrailer()
        )
        assertEquals(
            "https://www.youtube.com/watch?v=abcdefghijk",
            VideoDto(key = "abcdefghijk", site = "YouTube", type = "Trailer", official = true)
                .toOfficialTrailer()
                ?.url,
        )
    }

    @Test
    fun `untrusted image bases and paths are rejected`() {
        assertNull(images.poster("https://evil.example/a.jpg"))
        assertNull(images.poster("/../secret.jpg"))
        assertNull(
            TmdbImages(ImageConfigurationDto("http://image.tmdb.org/t/p/", listOf("w342")))
                .poster("/a.jpg")
        )
        assertNull(
            TmdbImages(
                    ImageConfigurationDto(
                        "https://image.tmdb.org.evil.example/t/p/",
                        listOf("w342"),
                    )
                )
                .poster("/a.jpg")
        )
        assertEquals("https://image.tmdb.org/t/p/w342/a.jpg", images.poster("/a.jpg"))
    }
}
