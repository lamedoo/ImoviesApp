package com.lukakordzaia.imoviesapp.ui.phone.videoplayer

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.navArgs
import com.google.android.exoplayer2.util.Util
import com.google.android.gms.cast.framework.CastButtonFactory
import com.lukakordzaia.imoviesapp.R
import com.lukakordzaia.imoviesapp.utils.setGone
import kotlinx.android.synthetic.main.exoplayer_controller_layout.*
import kotlinx.android.synthetic.main.exoplayer_controller_layout_new.*
import kotlinx.android.synthetic.main.fragment_video_player.*


class VideoPlayerFragment : Fragment(R.layout.fragment_video_player) {
    private lateinit var viewModel: VideoPlayerViewModel
    private val args: VideoPlayerFragmentArgs by navArgs()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        viewModel = ViewModelProvider(this).get(VideoPlayerViewModel::class.java)

        if (requireActivity().resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewModel.getPlaylistFiles(args.titleId, args.chosenSeason, args.chosenLanguage)
        }

        viewModel.episodeName.observe(viewLifecycleOwner, {
            header_tv.text = it
        })

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.setDecorFitsSystemWindows(false)
        } else {
            requireActivity().window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        }

    }

    override fun onStart() {
        super.onStart()
        if (Util.SDK_INT >= 24) {
            viewModel.initPlayer(requireContext(), title_player, args.isTvShow, args.watchedTime, args.chosenEpisode)
        }
    }

    override fun onResume() {
        super.onResume()
        if (Util.SDK_INT < 24) {
            viewModel.initPlayer(requireContext(), title_player, args.isTvShow, args.watchedTime, args.chosenEpisode)
        }
    }

    override fun onPause() {
        super.onPause()
        if (Util.SDK_INT < 24) {
            viewModel.releasePlayer()
            viewModel.saveTitleToDb(requireContext(), args.titleId, args.isTvShow, args.chosenLanguage)
        }
    }

    override fun onStop() {
        super.onStop()
        if (Util.SDK_INT >= 24) {
            viewModel.releasePlayer()
            viewModel.saveTitleToDb(requireContext(), args.titleId, args.isTvShow, args.chosenLanguage)
        }
    }

    override fun onDetach() {
        super.onDetach()
        requireActivity().requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            requireActivity().window.setDecorFitsSystemWindows(true)
        } else {
            requireActivity().window.decorView.systemUiVisibility = View.VISIBLE
        }
    }


    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.cast, menu)
        CastButtonFactory.setUpMediaRouteButton(requireContext(), menu, R.id.media_route_menu_item)
    }
}