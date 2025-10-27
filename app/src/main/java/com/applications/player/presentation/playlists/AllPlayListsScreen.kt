package com.applications.player.presentation.playlists

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment

@Composable
fun AllPlaylistsScreen(){
    Scaffold() {paddingValues ->

        Box(modifier = androidx.compose.ui.Modifier.padding(paddingValues), contentAlignment = Alignment.Center){
            Text(text = "No PlayLists Found")
        }


    }
}
