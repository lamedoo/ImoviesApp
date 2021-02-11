package com.lukakordzaia.medootv.ui.phone.videoplayer

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.android.exoplayer2.MediaItem
import com.lukakordzaia.medootv.database.ImoviesDatabase
import com.lukakordzaia.medootv.database.DbDetails
import com.lukakordzaia.medootv.datamodels.TitleData
import com.lukakordzaia.medootv.datamodels.TitleFiles
import com.lukakordzaia.medootv.datamodels.VideoPlayerInit
import com.lukakordzaia.medootv.datamodels.VideoPlayerRelease
import com.lukakordzaia.medootv.network.Result
import com.lukakordzaia.medootv.repository.SingleTitleRepository
import com.lukakordzaia.medootv.ui.baseclasses.BaseViewModel
import kotlinx.coroutines.launch

class VideoPlayerViewModel(private val repository: SingleTitleRepository) : BaseViewModel() {

    val isTvShow = MutableLiveData<Boolean>()
    val numOfSeasons = MutableLiveData<Int>()

    private var currentWindow = MutableLiveData<Int>(0)
    private var playbackPosition = MutableLiveData<Long>(0L)

    val episodesUri = MutableLiveData<MutableList<MediaItem>>()
    val playBackOptions = MutableLiveData<VideoPlayerInit>()

    private val seasonEpisodes: MutableList<String> = ArrayList()
    private val seasonEpisodesUri: MutableList<MediaItem> = ArrayList()

    val seasonForDb = MutableLiveData(1)
    private val episodeForDb = MutableLiveData<Int>()
    val titleIdForDb = MutableLiveData<Int>()
    val languageForDb = MutableLiveData<String>()
    private var titleDurationForDb = MutableLiveData<Long>(0L)

    private val getTitleNameList: MutableList<String> = mutableListOf()

    private val setTitleNameList = MutableLiveData<List<String>>()
    val setTitleName: LiveData<List<String>> = setTitleNameList

    private val _singleTitleData = MutableLiveData<TitleData.Data>()
    val singleTitleData: LiveData<TitleData.Data> = _singleTitleData

    fun getNumOfSeasons(numOfSeasons: Int) {
        this.numOfSeasons.value = numOfSeasons
    }

    fun initPlayer(isTvShow: Boolean, watchTime: Long, chosenEpisode: Int) {
        if (isTvShow) {
            this.isTvShow.value = isTvShow
            currentWindow.value = chosenEpisode - 1
        }
        playBackOptions.value = VideoPlayerInit(
            currentWindow.value!!,
            watchTime
        )
    }

    fun releasePlayer(videoPlayerRelease: VideoPlayerRelease) {
        playbackPosition.value = videoPlayerRelease.playbackPosition
        episodeForDb.value = videoPlayerRelease.currentWindow
        titleDurationForDb.value = videoPlayerRelease.titleDuration
    }

    fun saveTitleToDb(context: Context, titleId: Int, isTvShow: Boolean, chosenLanguage: String) {
        val database = ImoviesDatabase.getDatabase(context)?.getDao()
        viewModelScope.launch {
            if (playbackPosition.value!! > 0) {
                database?.insertWatchedTitle(
                    DbDetails(
                        titleId,
                        chosenLanguage,
                        playbackPosition.value!!,
                        titleDurationForDb.value!!,
                        isTvShow,
                        seasonForDb.value!!,
                        episodeForDb.value!! + 1
                    )
                )
            }
        }
    }

    private fun clearPlayerForNextSeason() {
        seasonEpisodes.clear()
        seasonEpisodesUri.clear()
        getTitleNameList.clear()
    }

    fun getPlaylistFiles(titleId: Int, chosenSeason: Int, chosenLanguage: String) {
        seasonForDb.value = chosenSeason
        titleIdForDb.value = titleId
        languageForDb.value = chosenLanguage
        clearPlayerForNextSeason()
        viewModelScope.launch {
            when (val files = repository.getSingleTitleFiles(titleId, chosenSeason)) {
                is Result.Success -> {
                    val season = files.data.data
                    season.forEach { singleEpisode ->
                        getTitleNameList.add(singleEpisode.title)
                        singleEpisode.files!!.forEach { singleFiles ->
                            checkAvailability(singleFiles, chosenLanguage)
                        }
                    }
                    seasonEpisodes.forEach {
                        val items = MediaItem.fromUri(Uri.parse(it))
                        seasonEpisodesUri.add(items)
                    }
                    setTitleNameList.value = getTitleNameList
                    episodesUri.value = seasonEpisodesUri
                    Log.d("episodesaaa", "${seasonEpisodes}")
                }
                is Result.Error -> {
                    Log.d("errornextepisode", files.exception)
                }
            }
        }

    }

    private fun checkAvailability(singleFiles: TitleFiles.Data.File, chosenLanguage: String) {
        if (singleFiles.lang == chosenLanguage) {
            if (singleFiles.files.size == 1) {
                if (singleFiles.files[0].quality == "MEDIUM") {
                    seasonEpisodes.add(singleFiles.files[0].src!!)
                } else {
                    seasonEpisodes.add(singleFiles.files[0].src!!)
                }
            } else if (singleFiles.files.size > 1) {
                singleFiles.files.forEach {
                    if (it.quality == "HIGH" && it.src != null) {
                        seasonEpisodes.add(it.src)
                    }
                }
            }
        }
    }

    fun getSingleTitleData(titleId: Int) {
        viewModelScope.launch {
            when (val data = repository.getSingleTitleData(titleId)) {
                is Result.Success -> {
                    _singleTitleData.value = data.data.data
                }
                is Result.Error -> {
                    Log.d("errorsinglemovies", data.exception)
                }
            }
        }
    }
}