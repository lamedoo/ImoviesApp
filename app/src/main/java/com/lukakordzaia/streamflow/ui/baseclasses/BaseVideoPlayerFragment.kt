package com.lukakordzaia.streamflow.ui.baseclasses

import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.text.CaptionStyleCompat
import com.google.android.exoplayer2.text.CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
import com.google.android.exoplayer2.ui.PlayerView
import com.google.android.exoplayer2.ui.SubtitleView
import com.lukakordzaia.streamflow.R
import com.lukakordzaia.streamflow.animations.VideoPlayerAnimations
import com.lukakordzaia.streamflow.datamodels.PlayerDurationInfo
import com.lukakordzaia.streamflow.datamodels.TitleMediaItemsUri
import com.lukakordzaia.streamflow.datamodels.VideoPlayerInfo
import com.lukakordzaia.streamflow.helpers.videoplayer.BuildMediaSource
import com.lukakordzaia.streamflow.helpers.videoplayer.MediaPlayerClass
import com.lukakordzaia.streamflow.helpers.videoplayer.VideoPlayerViewModel
import com.lukakordzaia.streamflow.ui.tv.TvActivity
import com.lukakordzaia.streamflow.ui.tv.tvvideoplayer.TvVideoPlayerActivity
import com.lukakordzaia.streamflow.utils.*
import kotlinx.android.synthetic.main.continue_watching_dialog.*
import kotlinx.android.synthetic.main.phone_exoplayer_controller_layout.*
import kotlinx.android.synthetic.main.phone_exoplayer_controller_layout.view.*
import kotlinx.android.synthetic.main.phone_fragment_video_player.*
import kotlinx.android.synthetic.main.tv_exoplayer_controller_layout.*
import kotlinx.android.synthetic.main.tv_exoplayer_controller_layout.view.*
import kotlinx.android.synthetic.main.tv_video_player_fragment.*
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.concurrent.TimeUnit


open class BaseVideoPlayerFragmentNew(fragment: Int) : Fragment(fragment) {
    protected val videoPlayerViewModel: VideoPlayerViewModel by viewModel()
    private val buildMediaSource: BuildMediaSource by inject()

    private lateinit var mediaPlayer: MediaPlayerClass
    protected lateinit var player: SimpleExoPlayer
    private lateinit var playerView: PlayerView
    private var tracker: ProgressTrackerNew? = null

    private var mediaItemsPlayed = 0
    private var isTrailer: Boolean = false
    private var episodeHasEnded = false

    private lateinit var videoPlayerInfo: VideoPlayerInfo

    private val autoBackPress = object : CountDownTimer(500000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
            }
            override fun onFinish() {
                if (this@BaseVideoPlayerFragmentNew.activity is TvVideoPlayerActivity) {
                    requireActivity().onBackPressed()
                    requireActivity().onBackPressed()
                } else {
                    requireActivity().onBackPressed()
                }
            }
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        player = SimpleExoPlayer.Builder(requireContext()).build()
        mediaPlayer = MediaPlayerClass(player)

        mediaPlayer.setPlayerListener(object : Player.EventListener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                super.onIsPlayingChanged(isPlaying)

                if (isPlaying) {
                    autoBackPress.cancel()
                } else {
                    autoBackPress.start()
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                super.onPlaybackStateChanged(state)

                if (isTrailer) {
                    phone_prev_button?.setGone()
                    phone_next_button?.setGone()
                }

                videoPlayerViewModel.videoPlayerInfo.observe(viewLifecycleOwner, {
                    videoPlayerInfo = it
                })

                if (this@BaseVideoPlayerFragmentNew::videoPlayerInfo.isInitialized) {
                    nextButtonClickListener()
                    prevButtonClickListener()
                }

                if (state == Player.STATE_READY) {

                    episodeHasEnded = true
                    showContinueWatchingDialog()
                    tracker = ProgressTrackerNew(player, object : ProgressTrackerNew.PositionListener {
                        override fun progress(position: Long) {
                            exo_live_duration?.text = String.format("%02d:%02d:%02d",
                                    TimeUnit.MILLISECONDS.toHours(position),
                                    TimeUnit.MILLISECONDS.toMinutes(position) -
                                            TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(position)),
                                    TimeUnit.MILLISECONDS.toSeconds(position) -
                                            TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(position))
                            )
                        }
                    })
                    playerView.keepScreenOn = true
                } else playerView.keepScreenOn = !(state == Player.STATE_IDLE || state == Player.STATE_ENDED)

                if (state == Player.STATE_ENDED) {
                    if (episodeHasEnded) {
                        mediaItemsPlayed++
                        tv_next_button?.callOnClick()
                        phone_next_button?.callOnClick()
                        episodeHasEnded = false
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Handler(Looper.getMainLooper()).postDelayed({
                    setTitleName()

                    videoPlayerViewModel.setVideoPlayerInfo(
                            PlayerDurationInfo(
                                    player.currentPosition,
                                    player.duration
                            )
                    )
                    if (mediaItem != null) {
                        if (!isTrailer) {
                            videoPlayerViewModel.addContinueWatching(requireContext())
                        }
                    }
                }, 2000)
            }
        })

        videoPlayerViewModel.toastMessage.observe(viewLifecycleOwner, EventObserver {
            requireContext().createToast(it, Toast.LENGTH_LONG)
        })
    }

    fun setExoPlayer(playerView: PlayerView) {
        this.playerView = playerView

        playerView.subtitleView?.apply {
            setInvisible()
        }
    }

    private fun prevButtonClickListener() {
        val prevButton: ImageButton? = phone_prev_button

        if (videoPlayerInfo.isTvShow) {
            if (videoPlayerInfo.chosenEpisode == 1) {
                prevButton?.setOnClickListener {
                    mediaItemsPlayed = 0
                    exo_prev?.callOnClick()
                }
            } else {
                prevButton?.setOnClickListener {
                    mediaItemsPlayed = 0
                    player.clearMediaItems()
                    videoPlayerViewModel.getTitleFiles(VideoPlayerInfo(
                            videoPlayerInfo.titleId,
                            videoPlayerInfo.isTvShow,
                            videoPlayerInfo.chosenSeason,
                            videoPlayerInfo.chosenEpisode-1,
                            videoPlayerInfo.chosenLanguage
                    ))
                    mediaPlayer.initPlayer(playerView, 0, 0L)
                }
            }
        } else {
            prevButton?.setGone()
        }
    }

    private fun nextButtonClickListener() {
        var nextButton: ImageButton? = null
        when (playerView) {
            tv_title_player -> {
                nextButton = tv_next_button
            }
            phone_title_player -> {
                nextButton = phone_next_button
            }
        }

        if (videoPlayerInfo.isTvShow) {
            videoPlayerViewModel.totalEpisodesInSeason.observe(viewLifecycleOwner, {
                if (videoPlayerInfo.chosenEpisode == it) {
                    nextButton?.setOnClickListener {
                        episodeHasEnded = false
                        player.clearMediaItems()
                        videoPlayerViewModel.getTitleFiles(VideoPlayerInfo(
                                videoPlayerInfo.titleId,
                                videoPlayerInfo.isTvShow,
                                videoPlayerInfo.chosenSeason+1,
                                1,
                                videoPlayerInfo.chosenLanguage
                        ))
                        mediaPlayer.initPlayer(playerView, 0, 0L)
                    }
                } else {
                    nextButton?.setOnClickListener {
                        episodeHasEnded = false
                        player.clearMediaItems()
                        videoPlayerViewModel.getTitleFiles(VideoPlayerInfo(
                                videoPlayerInfo.titleId,
                                videoPlayerInfo.isTvShow,
                                videoPlayerInfo.chosenSeason,
                                videoPlayerInfo.chosenEpisode+1,
                                videoPlayerInfo.chosenLanguage
                        ))
                        mediaPlayer.initPlayer(playerView, 0, 0L)
                    }
                }
            })
        } else {
            nextButton?.setGone()
        }
    }

    private fun setTitleName() {
        if (isTrailer) {
            header_tv?.text = "ტრეილერი"
            tv_header_tv?.text = "ტრეილერი"
        } else {
            videoPlayerViewModel.setTitleName.observe(viewLifecycleOwner, { name ->
                if (videoPlayerInfo.isTvShow) {
                    header_tv?.text = "ს${videoPlayerInfo.chosenSeason}. ე${videoPlayerInfo.chosenEpisode}. $name"
                    tv_header_tv?.text = "ს${videoPlayerInfo.chosenSeason}. ე${videoPlayerInfo.chosenEpisode}. $name"
                } else {
                    header_tv?.text = name
                    tv_header_tv?.text = name
                }
            })
        }
    }

    private fun subtitleFunctions(hasSubs: Boolean) {
        var subtitleView: SubtitleView? = null
        var subtitleToggle: ImageButton? = null
        when (playerView) {
            tv_title_player -> {
                subtitleView = tv_subtitle
                subtitleToggle = tv_subtitle_toggle
            }
            phone_title_player -> {
                subtitleView = phone_subtitle
                subtitleToggle = phone_subtitle_toggle
            }
        }

        player.addTextOutput {
            subtitleView?.onCues(it)

            if (view != null) {
                subtitleView?.setStyle(
                    CaptionStyleCompat(
                        ContextCompat.getColor(requireContext(), R.color.white),
                        ContextCompat.getColor(requireContext(), R.color.transparent),
                        ContextCompat.getColor(requireContext(), R.color.transparent),
                        EDGE_TYPE_DROP_SHADOW,
                        ContextCompat.getColor(requireContext(), R.color.black),
                        Typeface.DEFAULT_BOLD,
                    )
                )
                subtitleView?.setFixedTextSize(2, 25F)
            }
        }

        if (hasSubs) {
            subtitleToggle?.setVisible()
            subtitleToggle?.imageTintList = ColorStateList.valueOf(
                ContextCompat.getColor(
                    requireContext(),
                    R.color.accent_color
                )
            )
        } else {
            subtitleToggle?.setGone()
        }

        subtitleToggle?.setOnClickListener {
            if (subtitleView?.isVisible == true) {
                subtitleView.setInvisible()
                VideoPlayerAnimations().setSubtitleOff(subtitleToggle, 200, requireContext())
            } else {
                subtitleView?.setVisible()
                VideoPlayerAnimations().setSubtitleOn(subtitleToggle, 200, requireContext())
            }
        }
    }

    private fun showContinueWatchingDialog() {
        if (mediaItemsPlayed == 4) {
            videoPlayerViewModel.addContinueWatching(requireContext())
            player.pause()

            continue_watching_dialog_root.setVisible()

            continue_watching_dialog_yes.setOnClickListener {
                continue_watching_dialog_root.setGone()
                player.play()
            }

            if (this.activity is TvVideoPlayerActivity) {
                continue_watching_dialog_home.setOnClickListener {
                    val intent = Intent(requireContext(), TvActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                    this.startActivity(intent)
                }
            } else {
                continue_watching_dialog_home.setOnClickListener {
                    requireActivity().onBackPressed()
                }
            }
            continue_watching_dialog_yes.requestFocus()

            mediaItemsPlayed = 0
        }
    }



    fun getPlayListFiles(titleId: Int, chosenSeason: Int, chosenEpisode: Int, chosenLanguage: String, isTvShow: Boolean) {
        videoPlayerViewModel.getTitleFiles(VideoPlayerInfo(titleId, isTvShow, chosenSeason, chosenEpisode, chosenLanguage))
        videoPlayerViewModel.getSingleTitleData(titleId)
    }

    fun initPlayer(watchedTime: Long, trailerUrl: String?) {
        if (trailerUrl != null) {
            isTrailer = true
            mediaPlayer.setMediaItems(listOf(MediaItem.fromUri(Uri.parse(trailerUrl))))
            mediaPlayer.setPlayerMediaSource(buildMediaSource.movieMediaSource(TitleMediaItemsUri(MediaItem.fromUri(Uri.parse(trailerUrl)), "0")))
        } else {
            isTrailer = false
            videoPlayerViewModel.mediaAndSubtitle.observe(viewLifecycleOwner, {
                mediaPlayer.setPlayerMediaSource(buildMediaSource.movieMediaSource(it))

                if (it.titleSubUri.isNotEmpty()) {
                    if (it.titleSubUri == "0") {
                        subtitleFunctions(false)
                    } else {
                        subtitleFunctions(true)
                    }
                } else {
                    subtitleFunctions(false)
                }
            })
        }
        mediaPlayer.initPlayer(playerView, 0, watchedTime)
    }

    fun releasePlayer() {
        mediaPlayer.releasePlayer {
            videoPlayerViewModel.setVideoPlayerInfo(it)
        }
        tracker?.purgeHandler()
        if (!isTrailer) {
            videoPlayerViewModel.addContinueWatching(requireContext())
        }
    }

    override fun onDetach() {
        super.onDetach()
        autoBackPress.cancel()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }
}

class ProgressTrackerNew(private val player: Player, private val positionListener: PositionListener) : Runnable {
    interface PositionListener {
        fun progress(position: Long)
    }

    private val handler: Handler = Handler()
    override fun run() {
        val position = player.duration - player.currentPosition
        positionListener.progress(position)
        handler.postDelayed(this, 1000)
    }

    fun purgeHandler() {
        handler.removeCallbacks(this)
    }

    init {
        handler.post(this)
    }
}