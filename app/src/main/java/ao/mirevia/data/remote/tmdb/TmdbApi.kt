package ao.mirevia.data.remote.tmdb

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface TmdbApi {
    @GET("configuration") suspend fun configuration(): ConfigurationDto

    @GET("genre/{type}/list")
    suspend fun genres(
        @Path("type") type: String,
        @Query("language") language: String = "pt-PT",
    ): GenresDto

    @GET("{type}/{category}")
    suspend fun catalog(
        @Path("type") type: String,
        @Path("category") category: String,
        @Query("page") page: Int = 1,
        @Query("language") language: String = "pt-PT",
    ): MediaPageDto

    @GET("trending/all/week")
    suspend fun trending(@Query("language") language: String = "pt-PT"): MediaPageDto

    @GET("discover/{type}")
    suspend fun discover(
        @Path("type") type: String,
        @QueryMap filters: Map<String, String>,
        @Query("page") page: Int,
        @Query("language") language: String = "pt-PT",
    ): MediaPageDto

    @GET("search/{scope}")
    suspend fun search(
        @Path("scope") scope: String,
        @Query("query") query: String,
        @Query("page") page: Int,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("language") language: String = "pt-PT",
    ): MediaPageDto

    @GET("{type}/{id}")
    suspend fun details(
        @Path("type") type: String,
        @Path("id") id: Int,
        @Query("append_to_response")
        append: String = "credits,external_ids,videos,recommendations,similar",
        @Query("language") language: String = "pt-PT",
    ): MediaDetailsDto

    @GET("tv/{id}/season/{season}")
    suspend fun season(
        @Path("id") id: Int,
        @Path("season") season: Int,
        @Query("language") language: String = "pt-PT",
    ): SeasonDetailsDto

    @GET("person/{id}")
    suspend fun person(
        @Path("id") id: Int,
        @Query("append_to_response") append: String = "combined_credits",
        @Query("language") language: String = "pt-PT",
    ): PersonDetailsDto
}
