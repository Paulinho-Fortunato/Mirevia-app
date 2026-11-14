package ao.mirevia.playback

import ao.mirevia.core.playback.EmbedUrlPolicy
import ao.mirevia.core.playback.IframeRequestPolicy
import ao.mirevia.core.playback.VideoSource

/** An app-owned wrapper with one unmodified cross-origin iframe. No JavaScript or native bridge. */
object EmbedPage {
    const val HOST = IframeRequestPolicy.LOCAL_HOST
    const val URL = IframeRequestPolicy.LOCAL_DOCUMENT_URL

    fun html(source: VideoSource, policy: EmbedUrlPolicy): String {
        require(policy.isValidSource(source))
        return """
            <!doctype html>
            <html lang="pt"><head>
            <meta charset="utf-8">
            <meta name="viewport" content="width=device-width, initial-scale=1">
            <meta http-equiv="Content-Security-Policy" content="default-src 'none'; style-src 'unsafe-inline'; frame-src ${IframeRequestPolicy.FRAME_CSP_SOURCES}; object-src 'none'; base-uri 'none'; form-action 'none'; upgrade-insecure-requests">
            <title>Mirevia — player externo</title>
            <style>html,body{margin:0;width:100%;height:100%;background:#000;overflow:hidden}iframe{display:block;width:100%;height:100%;border:0}</style>
            </head><body>
            <iframe src="${source.url}" title="Player externo EmbedMovies" width="100%" height="100%" frameborder="0" scrolling="no" allow="autoplay; encrypted-media; picture-in-picture; fullscreen" allowfullscreen></iframe>
            </body></html>
        """
            .trimIndent()
    }
}
