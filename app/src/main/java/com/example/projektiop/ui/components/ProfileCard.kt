package com.example.projektiop.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.projektiop.R
import com.example.projektiop.data.util.isoToDisplayDate
import com.example.projektiop.domain.models.Gender
import com.example.projektiop.domain.models.User
import com.example.projektiop.domain.models.UserInterest
import com.example.projektiop.ui.screens.InfoItem
import com.example.projektiop.util.translateInterestName

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileCard(user: User?, interests: List<UserInterest>?, loading: Boolean, error: String?, modifier: Modifier = Modifier, onEditProfile: (() -> Unit)? = null) {

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val rawUrl = user?.profile?.avatarUrl?.takeIf { it.isNotBlank() }
                UserAvatar(rawUrl, modifier = Modifier.size(90.dp))

                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = when {
                        loading -> stringResource(R.string.profile_loading)
                        error != null -> stringResource(R.string.profile_error) + "\n $error"
                        user?.profile?.displayName?.isNotEmpty() == true -> user.profile.displayName
                        else -> stringResource(R.string.profile_name_placeholder)
                    },
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )
                if (onEditProfile != null) {
                    IconButton(onClick = { onEditProfile() }) {
                        Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit_profile))
                    }
                }
            }
            // --- Pasek z płcią, miastem i wiekiem ---
            val genderRaw = user?.profile?.gender
            val genderLabel = when (genderRaw) {
                Gender.MALE -> stringResource(R.string.gender_male)
                Gender.FEMALE -> stringResource(R.string.gender_female)
                Gender.OTHER -> stringResource(R.string.gender_other)
                Gender.PREFER_NOT_TO_SAY -> stringResource(R.string.gender_prefer_not_to_say)
                else -> null
            }
            val locationVal = user?.profile?.location?.takeIf { it.isNotBlank() }
            val formattedBirthDate = user?.profile?.birthDate?.let{isoToDisplayDate(it)}
            val infoItems = listOfNotNull(
                genderLabel?.let { InfoItem(Icons.Default.Person, it) },
                locationVal?.let { InfoItem(Icons.Default.LocationOn, it) },
                formattedBirthDate?.let { InfoItem(Icons.Default.Cake, it) }
            )
            if (infoItems.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = MaterialTheme.colorScheme.onSurfaceVariant
                    infoItems.forEachIndexed { index, item ->
                        if (index > 0) Spacer(Modifier.width(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(item.icon, contentDescription = null, modifier = Modifier.size(16.dp), tint = color)
                            Spacer(Modifier.width(4.dp))
                            Text(item.text, style = MaterialTheme.typography.bodySmall, color = color)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = when {
                    loading -> stringResource(R.string.profile_loading_description)
                    error != null -> error.takeIf { it.isNotBlank() } ?: stringResource(R.string.profile_error_description)
                    user?.profile?.bio?.isNotEmpty() == true -> user.profile.bio
                    else -> stringResource(R.string.profile_description_placeholder)
                },
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = stringResource(R.string.profile_interests_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (loading) {
                    InterestTag(base = "...")
                } else if (interests.isNullOrEmpty() ) {
                    InterestTag(base = stringResource(R.string.profile_no_interests), label = "")
                } else {
                    interests.forEach { ui ->
                        val rawBase = ui.interest.name.ifBlank { stringResource(R.string.profile_unknown_interest) }
                        val translatedBase = translateInterestName(rawBase)
                        val label = if (ui.customDescription.isNotBlank() && ui.customDescription != "null") ui.customDescription else ""
                        InterestTag(base = translatedBase, label = label)
                    }
                }
            }
        }
    }
}