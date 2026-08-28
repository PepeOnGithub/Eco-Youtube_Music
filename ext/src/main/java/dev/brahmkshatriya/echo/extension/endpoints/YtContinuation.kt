package dev.brahmkshatriya.echo.extension.endpoints

import kotlinx.serialization.Serializable

@Serializable
data class YtContinuation(
    val onResponseReceivedActions: List<OnResponseReceivedAction>? = null
) {
    @Serializable
    data class OnResponseReceivedAction(
        val appendContinuationItemsAction: AppendContinuationItemsAction? = null
    )

    @Serializable
    data class AppendContinuationItemsAction(
        val continuationItems: List<YoutubeiBrowseResponse.YoutubeiShelf.YoutubeiShelfContentsItem>? = null
    )
}