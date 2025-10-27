package com.applications.player.presentation.homeScreen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class HomeActivityViewModel : ViewModel() {

    private val _homeActivityModel = MutableStateFlow(HomeActivityState(itemSelected = NavItem.VIDEOS))
    val homeActivityState: StateFlow<HomeActivityState> = _homeActivityModel.asStateFlow()

    // Public function to handle UI events (clicks)
    fun onNavigationItemSelected(item: NavItem) {
        _homeActivityModel.update { currentState ->
            currentState.copy(itemSelected = item)
        }
    }


}

