package com.lukakordzaia.streamflow.ui.phone.videoplayer

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.MediaInfo
import com.google.android.gms.cast.MediaLoadRequestData
import com.google.android.gms.cast.MediaMetadata
import com.google.android.gms.cast.MediaTrack
import com.google.android.gms.cast.framework.*
import com.google.android.gms.cast.framework.media.RemoteMediaClient
import com.lukakordzaia.streamflow.R
import com.lukakordzaia.streamflow.datamodels.VideoPlayerData
import com.lukakordzaia.streamflow.ui.baseclasses.BaseVideoPlayerFragmentNew
import kotlinx.android.synthetic.main.phone_exoplayer_controller_layout.*
import kotlinx.android.synthetic.main.phone_fragment_video_player.*
import java.util.*


class VideoPlayerFragment : BaseVideoPlayerFragmentNew(R.layout.phone_fragment_video_player),
    CastStateListener {
    private lateinit var videoPlayerData: VideoPlayerData
    private var castSession: CastSession? = null
    private lateinit var sessionManager: SessionManager
    private val sessionManagerListener = object : SessionManagerListener<CastSession> {
        override fun onSessionStarting(session: CastSession?) {

        }

        override fun onSessionStarted(session: CastSession?, sessionId: String?) {
            onApplicationConnected(session)
        }

        override fun onSessionStartFailed(session: CastSession?, error: Int) {
            onApplicationDisconnected()
        }

        override fun onSessionEnding(session: CastSession?) {
            onApplicationDisconnected()
        }

        override fun onSessionEnded(session: CastSession?, error: Int) {

        }

        override fun onSessionResuming(session: CastSession?, sessionId: String?) {

        }

        override fun onSessionResumed(session: CastSession?, boolean: Boolean) {
            onApplicationConnected(session)
        }

        override fun onSessionResumeFailed(session: CastSession?, error: Int) {
            onApplicationDisconnected()
        }

        override fun onSessionSuspended(session: CastSession?, reason: Int) {

        }

        private fun onApplicationConnected(session: CastSession?) {
            castSession = session
            player.currentMediaItem?.let {
                loadRemoteMedia(
                    buildMediaInfo(
                        it.mediaId,
                        videoPlayerViewModel.mediaAndSubtitle.value?.titleSubUri
                    ), player.currentPosition
                )
                player.pause()
            }
        }

        private fun onApplicationDisconnected() {
            player.play()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        videoPlayerData = activity?.intent!!.getParcelableExtra("videoPlayerData")

        setExoPlayer(phone_title_player)

        phone_exo_back.setOnClickListener {
            requireActivity().onBackPressed()
        }

        if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (videoPlayerData.trailerUrl == null) {
                getPlayListFiles(
                    videoPlayerData.titleId,
                    videoPlayerData.chosenSeason,
                    videoPlayerData.chosenEpisode,
                    videoPlayerData.chosenLanguage,
                    videoPlayerData.isTvShow
                )
            }
        }

        setupCastPlayer()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)

        requireActivity().window.decorView.apply {
            systemUiVisibility =
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                initPlayer(videoPlayerData.watchedTime, videoPlayerData.trailerUrl)
            }
            Log.d("videoplaying", "started")
        }
    }

    override fun onResume() {
        super.onResume()
        castSession = sessionManager.currentCastSession
        sessionManager.addSessionManagerListener(sessionManagerListener, CastSession::class.java)
        if (Util.SDK_INT < 24) {
            if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                initPlayer(videoPlayerData.watchedTime, videoPlayerData.trailerUrl)
            }
            Log.d("videoplaying", "resumed")
        }
    }

    override fun onPause() {
        super.onPause()
        sessionManager.removeSessionManagerListener(sessionManagerListener, CastSession::class.java)
        castSession = null
        if (Util.SDK_INT < 24) {
            releasePlayer()
            Log.d("videoplaying", "paused")
        }
    }

    override fun onStop() {
        if (Util.SDK_INT >= 24) {
            releasePlayer()
        }
        requireActivity().onBackPressed()
        Log.d("videoplaying", "stopped")
        super.onStop()
    }

    private fun setupCastPlayer() {
        CastButtonFactory.setUpMediaRouteButton(requireContext(), media_route_button)
        val castContext = CastContext.getSharedInstance(requireActivity())
        sessionManager = castContext.sessionManager

        if (castContext.castState != CastState.NO_DEVICES_AVAILABLE) {
            media_route_button.isVisible = true
        }
        castContext.addCastStateListener(this)


    }

    override fun onCastStateChanged(state: Int) {
        // sometimes MediaRouteButton creation is not finished and might be null
        if (media_route_button == null) {
            return
        }
        if (state == CastState.NO_DEVICES_AVAILABLE) {
            media_route_button.isVisible = false
        } else {
            if (media_route_button.visibility == View.GONE) {
                media_route_button.isVisible = true
            }
        }
    }

    private fun loadRemoteMedia(mediaInfo: MediaInfo, position: Long) {
        val remoteMediaClient: RemoteMediaClient = castSession?.remoteMediaClient ?: return
        remoteMediaClient.load(
            MediaLoadRequestData.Builder()
                .setMediaInfo(mediaInfo)
                .setActiveTrackIds(LongArray(1) { SUBTITLE_ACTIVE_TRACK_ID })
                .setAutoplay(true)
                .setCurrentTime(position)
                .build()
        )
    }

    private fun buildMediaInfo(videoUrl: String, subtitleUrl: String?): MediaInfo {
        val movieMetadata = MediaMetadata(MediaMetadata.MEDIA_TYPE_MOVIE)

        val mediaTrack = MediaTrack.Builder(SUBTITLE_ACTIVE_TRACK_ID, MediaTrack.TYPE_TEXT)
            .setName(videoPlayerData.chosenLanguage)
            .setSubtype(MediaTrack.SUBTYPE_SUBTITLES)
            .setContentId(subtitleUrl)
            .setLanguage(Locale.getDefault())
            .build()

        return MediaInfo.Builder(videoUrl)
            .setMediaTracks(listOf(mediaTrack))
            .setStreamType(MediaInfo.STREAM_TYPE_BUFFERED)
            .setContentType(MimeTypes.VIDEO_MP4)
            .setMetadata(movieMetadata)
            .build()
    }

    companion object {
        const val SUBTITLE_ACTIVE_TRACK_ID = 1L
    }
}