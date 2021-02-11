package com.lukakordzaia.medootv.ui.phone.categories

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.lukakordzaia.medootv.datamodels.GenreList
import com.lukakordzaia.medootv.datamodels.StudioList
import com.lukakordzaia.medootv.network.Result
import com.lukakordzaia.medootv.repository.CategoriesRepository
import com.lukakordzaia.medootv.ui.baseclasses.BaseViewModel
import kotlinx.coroutines.launch

class CategoriesViewModel(private val repository: CategoriesRepository) : BaseViewModel() {

    private val _allGenresList = MutableLiveData<List<GenreList.Data>>()
    val allGenresList: LiveData<List<GenreList.Data>> = _allGenresList

    private val _topStudioList = MutableLiveData<List<StudioList.Data>>()
    val topStudioList: LiveData<List<StudioList.Data>> = _topStudioList

    fun onSingleGenrePressed(genreId: Int) {
        navigateToNewFragment(CategoriesFragmentDirections.actionCategoriesFragmentToSingleGenreFragment(genreId))
    }

    fun onSingleStudioPressed(studioId: Int) {
        navigateToNewFragment(CategoriesFragmentDirections.actionCategoriesFragmentToSingleStudioFragment(studioId))
    }

    fun getAllGenres() {
        viewModelScope.launch {
            when (val genres = repository.getAllGenres()) {
                is Result.Success -> {
                    _allGenresList.value = genres.data.data
                    setLoading(false)
                }
                is Result.Error -> {
                    Log.d("errorgenres", genres.exception)
                }
            }
        }
    }

    fun getTopStudios() {
        viewModelScope.launch {
            when (val studios = repository.getTopStudios()) {
                is Result.Success -> {
                    _topStudioList.value = studios.data.data
                    setLoading(false)
                }
                is Result.Error -> {
                    Log.d("errorgenres", studios.exception)
                }
            }
        }
    }
}