package com.example.projektiop.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.projektiop.R
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.projektiop.data.util.isoToDisplayDate
import com.example.projektiop.domain.models.Gender
import com.example.projektiop.ui.components.BottomNavigationBar
import com.example.projektiop.ui.components.InterestTag
import com.example.projektiop.ui.components.ProfileCard
import com.example.projektiop.ui.components.PullToRefresh
import com.example.projektiop.ui.components.UserAvatar
import com.example.projektiop.ui.viewmodels.MainViewModel
import org.koin.androidx.compose.koinViewModel
import com.example.projektiop.util.translateInterestName


internal data class InfoItem(val icon: ImageVector, val text: String)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val viewModel = koinViewModel<MainViewModel>()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val loading by viewModel.loading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val user by viewModel.user.collectAsState()
    val interests by viewModel.myInterests.collectAsState()

    Scaffold(
        bottomBar = {
            BottomNavigationBar(navController = navController, currentRoute = currentRoute)
        }
    ) { paddingValues ->
        val isLoading by viewModel.loading.collectAsState()

        PullToRefresh(
            refreshing = isLoading,
            onRefresh = { viewModel.getMyProfile() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Karta profilu
            ProfileCard(
                onEditProfile = { navController.navigate("edit_profile") },
                user = user,
                interests = interests,
                loading = loading,
                error = error
            )

            Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    MaterialTheme {
        MainScreen(navController = rememberNavController())
    }
}
