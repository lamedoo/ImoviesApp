package com.lukakordzaia.streamflow.ui.baseclasses

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.navigation.NavDirections
import com.lukakordzaia.streamflow.utils.Event

abstract class BaseViewModel : ViewModel() {
    private val _navigateScreen = MutableLiveData<Event<NavDirections>>()
    val navigateScreen: LiveData<Event<NavDirections>> = _navigateScreen

    private val _toastMessage = MutableLiveData<Event<String>>()
    val toastMessage: LiveData<Event<String>> = _toastMessage

    private val _isLoading = MutableLiveData<Event<Boolean>>()
    val isLoading: LiveData<Event<Boolean>> = _isLoading

    private val _noInternet = MutableLiveData<Event<Boolean>>(Event(false))
    val noInternet: LiveData<Event<Boolean>> = _noInternet



    fun navigateToNewFragment(navId: NavDirections) {
        _navigateScreen.value = Event(navId)
    }

    fun newToastMessage(message: String) {
        _toastMessage.value = Event(message)
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = Event(loading)
    }

    fun setNoInternet() {
        _noInternet.value = Event(true)
    }
}