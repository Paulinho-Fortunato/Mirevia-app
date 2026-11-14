package ao.mirevia.core.model

import org.junit.Assert.*
import org.junit.Test

class DiscoverFiltersTest {
    @Test
    fun `different year parameter per media type`() {
        val filters =
            DiscoverFilters(genreId = 18, year = 2024, minimumRating = 7, originalLanguage = "pt")
        val movie = filters.toQuery(MediaType.MOVIE)
        assertEquals("2024", movie["primary_release_year"])
        assertFalse(movie.containsKey("first_air_date_year"))
        assertEquals("2024", filters.toQuery(MediaType.TV)["first_air_date_year"])
        assertEquals("pt", movie["with_original_language"])
        assertFalse(movie.containsKey("query"))
    }

    @Test
    fun `keys include type and origin`() {
        val movie = MediaKey(DataOrigin.TMDB, MediaType.MOVIE, 1)
        assertNotEquals(movie.storageKey, MediaKey(DataOrigin.TMDB, MediaType.TV, 1).storageKey)
        assertNotEquals(movie.storageKey, MediaKey(DataOrigin.DEMO, MediaType.MOVIE, 1).storageKey)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects invalid rating`() {
        DiscoverFilters(minimumRating = 11)
    }
}
