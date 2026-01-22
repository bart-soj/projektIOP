package com.example.projektiop.ui.screens

import android.widget.Space
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.projektiop.R
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.ui.res.stringResource
import com.example.projektiop.domain.models.Gender
import com.example.projektiop.ui.components.GlassPanel
import com.example.projektiop.ui.components.InterestTag
import com.example.projektiop.ui.components.UserAvatar
import com.example.projektiop.util.translateInterestName
import com.example.projektiop.ui.viewmodels.FriendProfileViewModel
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FriendProfileScreen(
    navController: NavController,
    userId: String,
    usernamePrefill: String? = null,
    displayNamePrefill: String? = null,
    avatarUrlPrefill: String? = null
) {
    val viewModel = koinViewModel<FriendProfileViewModel>(
        parameters = { parametersOf(userId, usernamePrefill, displayNamePrefill, avatarUrlPrefill) })
    val loading by viewModel.loading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val user by viewModel.profile.collectAsState()
    val interests by viewModel.interests.collectAsState()

    val effectiveDisplayName = user.profile.displayName

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = if (effectiveDisplayName.isNotBlank()) effectiveDisplayName
                    else stringResource(R.string.profile))
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back)) }
                }
            )
        }
    ) { paddingValues ->
        if (loading) {
            Box(Modifier.fillMaxSize().padding(paddingValues)) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        } else if (error != null) {
            Box(Modifier.fillMaxSize().padding(paddingValues)) { Text(error ?: stringResource(R.string.error), color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center)) }
        } else if (user == null) {
            Box(Modifier.fillMaxSize().padding(paddingValues)) { Text(stringResource(R.string.no_data), modifier = Modifier.align(Alignment.Center)) }
        } else {
                val p = user
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    GlassPanel {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            val rawUrl = p.profile.avatarUrl.takeIf { it.isNotBlank() }
                            UserAvatar(rawUrl, modifier = Modifier.size(90.dp))
                            Spacer(Modifier.width(16.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = if(effectiveDisplayName.isNotBlank()) effectiveDisplayName
                                           else stringResource(R.string.no_name),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                val age = p.profile.birthDate?.let { bd ->
                                    val datePart = bd.take(10)
                                    try {
                                        val ld = LocalDate.parse(
                                            datePart,
                                            DateTimeFormatter.ISO_DATE
                                        ); Period.between(
                                            ld,
                                            LocalDate.now()
                                        ).years.takeIf { it in 0..150 }
                                    } catch (_: Exception) {
                                        null
                                    }
                                }
                                val gender = when (p.profile.gender) {
                                    Gender.MALE -> stringResource(R.string.gender_male)
                                    Gender.FEMALE -> stringResource(R.string.gender_female)
                                    Gender.OTHER -> stringResource(R.string.gender_other)
                                    Gender.PREFER_NOT_TO_SAY -> stringResource(R.string.gender_prefer_not_to_say)
                                    else -> null
                                }
                                val location = p.profile.location
                                val infoLine =
                                    listOfNotNull(gender, location, age?.let { "$it l." }).joinToString(
                                        " • "
                                    )
                                if (infoLine.isNotBlank()) Text(
                                    infoLine,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (p.profile.bio.isNotBlank()) {
                            Text(p.profile.bio, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(Modifier.height(4.dp))
                        if (!interests.isNullOrEmpty()) {
                            Column {
                                Text(
                                    text = stringResource(R.string.interests),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(4.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    interests!!.forEach { ui ->
                                        val rawBase = ui.interest.name.ifBlank { stringResource(R.string.profile_unknown_interest) }
                                        val translatedBase = translateInterestName(rawBase)
                                        val label = if( ui.customDescription == "null" ) "" else ui.customDescription
                                        InterestTag(base = translatedBase, label = label)
                                    }
                                }
                            }
                        }
                }
            }
        }
    }
}
