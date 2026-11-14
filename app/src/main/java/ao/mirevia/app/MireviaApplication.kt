package ao.mirevia.app

import android.app.Application
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.svg.SvgDecoder
import okio.Path.Companion.toPath

class MireviaApplication : Application(), SingletonImageLoader.Factory {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun newImageLoader(context: android.content.Context): ImageLoader =
        ImageLoader.Builder(context)
            .components {
                add(OkHttpNetworkFetcherFactory())
                add(SvgDecoder.Factory())
            }
            .memoryCache { MemoryCache.Builder().maxSizeBytes(32L * 1024 * 1024).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("mirevia_images").absolutePath.toPath())
                    .maxSizeBytes(48L * 1024 * 1024)
                    .build()
            }
            .build()
}
