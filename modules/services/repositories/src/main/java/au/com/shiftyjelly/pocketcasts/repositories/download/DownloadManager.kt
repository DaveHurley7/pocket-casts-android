package au.com.shiftyjelly.pocketcasts.repositories.download

import android.content.Context
import androidx.core.app.NotificationCompat
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import io.reactivex.subjects.Subject

interface DownloadManager {
    companion object {
        const val WORK_MANAGER_DOWNLOAD_TAG = "downloadTask"
    }

    val progressUpdates: Map<String, DownloadProgressUpdate>
    val progressUpdateRelay: Subject<DownloadProgressUpdate>

    fun setup(episodeManager: EpisodeManager, podcastManager: PodcastManager, playlistManager: PlaylistManager, playbackManager: PlaybackManager)
    fun beginMonitoringWorkManager(context: Context)
    fun hasPendingOrRunningDownloads(): Boolean
    fun addEpisodeToQueue(episode: Episode, from: String, fireEvent: Boolean)
    fun removeEpisodeFromQueue(episode: Episode, from: String)
    fun stopAllDownloads()
    suspend fun getRequirementsAndSetStatusAsync(episode: Episode): NetworkRequirements
    fun getNotificationBuilder(): NotificationCompat.Builder
}
