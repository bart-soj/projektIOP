package com.example.projektiop.data.repositories

import android.util.Log
import com.example.projektiop.data.db.realm.RealmDataSource
import com.example.projektiop.data.mapping.toDomain
import com.example.projektiop.domain.models.SearchProfile
import com.example.projektiop.domain.DataError
import com.example.projektiop.domain.Result

class SearchProfileRepository(private val databaseDataSource: RealmDataSource,
                              private val userRepository: UserRepository) {

    suspend fun addSearchProfile(searchProfile: SearchProfile): Result<SearchProfile, DataError.Local> {
        try {
            databaseDataSource.addSearchProfile(searchProfile)
            return Result.Success(searchProfile)
        } catch(e: Exception) {
            Log.d("SP", "failed to add $e")
            return Result.Error(DataError.Local.DB_ERROR)
        }
    }

    suspend fun getSearchProfiles(): Result<List<SearchProfile>, DataError.Local> {
        try {
            val realmSearchProfiles = databaseDataSource.getSearchProfiles()
            val searchProfiles = realmSearchProfiles.map {
                it.toDomain()
            }
            return Result.Success(searchProfiles)
        } catch(e: Exception) {
            Log.d("SP", "failed to search $e")
            return Result.Error(DataError.Local.DB_ERROR)
        }
    }

    suspend fun deleteSearchProfile(toDelete: SearchProfile): Result<Unit, DataError.Local> {
        try {
            databaseDataSource.deleteSearchProfileByName(toDelete.name)
            return Result.Success(Unit)
        } catch(e: Exception) {
            Log.d("SP", "failed to delete $e")
            return Result.Error(DataError.Local.DB_ERROR)
        }
    }

    suspend fun getDefaultSearchProfile(): SearchProfile {
        // turns current interests of logged-in user into a search profile
        val interests = userRepository.MyUserInterests.value?.map { it.interest }
        return SearchProfile("Default", interests ?: emptyList())
    }
}