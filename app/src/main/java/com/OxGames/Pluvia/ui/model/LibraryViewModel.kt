package com.OxGames.Pluvia.ui.model

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.OxGames.Pluvia.data.LibraryItem
import com.OxGames.Pluvia.db.dao.SteamAppDao
import com.OxGames.Pluvia.enums.AppType
import com.OxGames.Pluvia.service.SteamService
import com.OxGames.Pluvia.ui.data.LibraryState
import com.OxGames.Pluvia.ui.enums.FabFilter
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val steamAppDao: SteamAppDao,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state.asStateFlow()

    // Keep the library scroll state. This will last longer as the VM will stay alive.
    var listState: LazyListState by mutableStateOf(LazyListState(0, 0))

    init {
        observeAppList()
    }

    fun onIsSearching(value: Boolean) {
        _state.update { it.copy(isSearching = value) }

        if (!value) {
            observeAppList()
        }
    }

    fun onSearchQuery(value: String) {
        _state.update { it.copy(searchQuery = value) }

        observeAppList()
    }

    fun onFabFilter(value: FabFilter) {
        _state.update { currentState ->
            val updatedFilter = currentState.appInfoSortType
            if (updatedFilter.contains(value)) {
                updatedFilter.remove(value)
            } else {
                updatedFilter.add(value)
            }
            currentState.copy(appInfoSortType = updatedFilter)
        }

        observeAppList()
    }

    private fun observeAppList() {
        viewModelScope.launch(Dispatchers.IO) {
            // clears out the list to prepare to load the new one
            _state.update { it.copy(appInfoList = emptyList()) }
            SteamService.userSteamId?.accountID?.toInt()?.let { steamId ->
                steamAppDao.getAllOwnedApps(
                    ownerId = steamId,
                    filter = AppType.code(FabFilter.getAppType(_state.value.appInfoSortType)),
                ).collect { apps ->
                    _state.update { currentState ->
                        val sortedList = apps
                            // filter out spacewar
                            .asSequence()
                            .filter { it.id != 480 }
                            .filter {
                                if (currentState.appInfoSortType.contains(FabFilter.INSTALLED)) SteamService.isAppInstalled(it.id) else true
                            }
                            .filter { it.name.contains(currentState.searchQuery, true) }
                            // TODO: include other sort types
                            .sortedBy { appInfo -> appInfo.name.lowercase() }
                            .mapIndexed { idx, item ->
                                // Slim down the list with only the necessary values.
                                LibraryItem(
                                    index = idx,
                                    appId = item.id,
                                    name = item.name,
                                    iconHash = item.clientIconHash,
                                )
                            }
                            .toList()

                        currentState.copy(appInfoList = sortedList)
                    }
                }
            }
        }
    }
}
