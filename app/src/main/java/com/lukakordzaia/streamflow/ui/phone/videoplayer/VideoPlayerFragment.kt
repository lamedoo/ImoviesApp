package com.lukakordzaia.streamflow.ui.phone.videoplayer

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.ext.cast.CastPlayer
import com.google.android.exoplayer2.ext.cast.SessionAvailabilityListener
import com.google.android.exoplayer2.util.MimeTypes
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.framework.CastButtonFactory
import com.google.android.gms.cast.framework.CastContext
import com.google.android.gms.cast.framework.CastState
import com.google.android.gms.cast.framework.CastStateListener
import com.lukakordzaia.streamflow.R
import com.lukakordzaia.streamflow.datamodels.VideoPlayerData
import com.lukakordzaia.streamflow.ui.baseclasses.BaseVideoPlayerFragmentNew
import kotlinx.android.synthetic.main.phone_exoplayer_controller_layout.*
import kotlinx.android.synthetic.main.phone_fragment_video_player.*


class VideoPlayerFragment : BaseVideoPlayerFragmentNew(R.layout.phone_fragment_video_player),
    SessionAvailabilityListener, CastStateListener {
    private lateinit var videoPlayerData: VideoPlayerData
    private lateinit var castPlayer: CastPlayer


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
        if (Util.SDK_INT < 24) {
            if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                initPlayer(videoPlayerData.watchedTime, videoPlayerData.trailerUrl)
            }
            Log.d("videoplaying", "resumed")
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            releasePlayer()
            castPlayer.release()
            Log.d("videoplaying", "paused")
        }
    }

    override fun onStop() {
        if (Util.SDK_INT >= 24) {
            releasePlayer()
            castPlayer.release()
        }
        requireActivity().onBackPressed()
        Log.d("videoplaying", "stopped")
        super.onStop()
    }

    private fun setupCastPlayer() {
        CastButtonFactory.setUpMediaRouteButton(requireContext(), media_route_button)
        val castContext = CastContext.getSharedInstance(requireActivity())

        if (castContext.castState != CastState.NO_DEVICES_AVAILABLE) {
            media_route_button.isVisible = true
        }
        castContext.addCastStateListener(this)

        castPlayer = CastPlayer(castContext).also {
            it.setSessionAvailabilityListener(this)
        }
    }

    override fun onCastSessionAvailable() {
        Log.d("CAST", "onCastSessionAvailable")
        player.currentMediaItem?.let {
            castPlayer.setMediaItem(buildMediaItem(it.mediaId), player.currentPosition)
            player.pause()
        }
    }

    override fun onCastSessionUnavailable() {
        player.play()
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

    private fun buildMediaItem(videoUrl: String): MediaItem {
        return MediaItem.Builder()
            .setUri(videoUrl)
            .setMimeType(MimeTypes.VIDEO_MP4)
            .build()
    }
}