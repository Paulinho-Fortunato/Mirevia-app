package ao.mirevia.core.playback

/** Consented popup documents, not initial playback/share sources or the main iframe wrapper. */
object PlayerWindowPolicy {
    const val BLANK_DOCUMENT = "about:blank"
    const val DESCRIPTION =
        "Janelas do player: HTTPS (443), qualquer domínio remoto, dentro da Mirevia."

    // A newly transported window starts as a local blank context. Never load a URL before
    // transport.
    fun isAllowedRequest(url: String): Boolean =
        url == BLANK_DOCUMENT || IframeRequestPolicy.isAllowedRequest(url, isMainFrame = false)
}
