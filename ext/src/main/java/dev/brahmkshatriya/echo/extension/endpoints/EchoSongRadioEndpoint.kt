package dev.brahmkshatriya.echo.extension.endpoints

import dev.toastbits.ytmkt.impl.youtubei.YoutubeiApi
import dev.toastbits.ytmkt.model.ApiEndpoint
import dev.toastbits.ytmkt.model.external.mediaitem.YtmSong
import io.ktor.client.call.body
import io.ktor.client.request.request
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject

private const val RADIO_ID_PREFIX = "RDAMVM"

/**
 * Local replacement for ytm-kt's YTMSongRadioEndpoint. That endpoint's response model
 * declares YoutubeiNextResponse.Content.musicQueueRenderer as required, but only the tab
 * that actually carries the queue has it - other tabs in the same "next" response (lyrics,
 * related) have their `content` shaped differently, so eagerly deserializing the whole tab
 * list throws on any video where those other tabs are present. Reuses EchoSongEndPoint's own
 * local copy of this response shape (already made defensive against the same issue) instead
 * of ytm-kt's.
 */
class EchoSongRadioEndpoint(override val api: YoutubeiApi) : ApiEndpoint() {

    data class RadioData(
        val items: List<YtmSong>,
        val continuation: String?
    )

    suspend fun getSongRadio(songId: String, continuation: String?): Result<RadioData> = runCatching {
        val response: HttpResponse = api.client.request {
            endpointPath("next")
            addApiHeadersWithAuthenticated()
            postWithBody {
                put("enablePersistentPlaylistPanel", true)
                put("tunerSettingValue", "AUTOMIX_SETTING_NORMAL")
                put("playlistId", RADIO_ID_PREFIX + songId)
                put("isAudioOnly", true)
                putJsonObject("watchEndpointMusicSupportedConfigs") {
                    putJsonObject("watchEndpointMusicConfig") {
                        put("hasPersistentPlaylistPanel", true)
                        put("musicVideoType", "MUSIC_VIDEO_TYPE_ATV")
                    }
                }
                if (continuation != null) {
                    put("continuation", continuation)
                }
            }
        }

        val panel: YoutubeiNextResponse.PlaylistPanelRenderer? = if (continuation == null) {
            val data: YoutubeiNextResponse = response.body()
            data.contents.singleColumnMusicWatchNextResultsRenderer.tabbedRenderer
                .watchNextTabbedResultsRenderer.tabs
                .firstNotNullOfOrNull {
                    it.tabRenderer.content?.musicQueueRenderer?.content?.playlistPanelRenderer
                }
        } else {
            val data: YoutubeiNextContinuationResponse = response.body()
            data.continuationContents.playlistPanelContinuation
        }

        RadioData(
            panel?.contents.orEmpty().map { item ->
                val renderer = item.getRenderer()
                YtmSong(
                    YtmSong.cleanId(renderer.videoId),
                    name = renderer.title.first_text,
                    artists = renderer.getArtists().getOrNull()
                )
            },
            panel?.continuations?.firstNotNullOfOrNull { it.data }?.continuation
        )
    }
}
