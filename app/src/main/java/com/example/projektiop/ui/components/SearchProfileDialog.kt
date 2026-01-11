package com.example.projektiop.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.projektiop.ui.viewmodels.ScannerViewModel
import com.example.projektiop.R
import com.example.projektiop.domain.models.Interest


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchProfileDialog(
    viewModel: ScannerViewModel,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {

    val searchProfileList by viewModel.searchProfileList.collectAsState()
    val errorMessage by viewModel.searchProfileError.collectAsState()
    val searchText by viewModel.searchSearchProfileText.collectAsState()
    val loading by viewModel.searchProfileLoading.collectAsState()
    val searchProfile by viewModel.searchProfile.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshSearchProfileList()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (showAddDialog) {
            Column(modifier = Modifier.padding(16.dp)) {
                var name by remember { mutableStateOf("") }
                var selectedInterests by remember { mutableStateOf<Set<Interest>>(emptySet()) }
                val allInterests by viewModel.publicInterests.collectAsState()

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(onClick = { showAddDialog = false }) {
                        Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = null)
                    }

                    // Save Button
                    IconButton(
                        onClick = {
                            viewModel.addSearchProfile(name, selectedInterests)
                            showAddDialog = false
                        },
                        enabled = name.isNotBlank() && !searchProfileList.map { it.name }.contains(name)
                    ) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    }
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 50) name = it },
                    label = { Text(stringResource(id = R.string.name_label)) },
                    supportingText = { Text(stringResource(id = R.string.nickname_count, name.length)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                MultiSelectInterestsDropdown(
                    all = allInterests,
                    selected = selectedInterests,
                    onChange = { set -> selectedInterests = set }
                )
            }
        } else {
            Column(modifier = Modifier.fillMaxSize()) {

                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth().padding(8.dp)
                ) {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(imageVector = Icons.Filled.Add, contentDescription = null)
                    }
                    IconButton(onClick = { viewModel.refreshSearchProfileList() }) {
                        Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                    }
                    IconButton(onClick = onClose) {
                        Icon(imageVector = Icons.Filled.ChevronLeft, contentDescription = null)
                    }
                }

                SearchBar(
                    searchText = searchText,
                    onSearchTextChanged = { viewModel.searchForSearchProfile(it) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                when {
                    loading -> {
                        Box(Modifier.fillMaxSize()) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    }

                    errorMessage != null -> {
                        Box(Modifier.fillMaxSize()) {
                            Column(
                                Modifier.align(Alignment.Center),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    errorMessage ?: stringResource(R.string.error),
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { viewModel.refreshSearchProfileList() }) {
                                    Text(stringResource(R.string.retry))
                                }
                            }
                        }
                    }

                    else -> {
                        SearchProfileItem(
                            searchProfile = searchProfile,
                            containerColor = Color.Green.copy(alpha = 0.4f), // Green tint
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        if (searchProfileList.isEmpty()) {
                            Box(Modifier.fillMaxSize()) {
                                Text(
                                    stringResource(R.string.no_search_profiles),
                                    Modifier.align(Alignment.Center)
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(searchProfileList, key = { it.name }) { item ->
                                    SearchProfileItem(
                                        searchProfile = item,
                                        onDeleteClick = { viewModel.deleteSearchProfile(item) },
                                        onChooseClick = { viewModel.chooseSearchProfile(item) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
