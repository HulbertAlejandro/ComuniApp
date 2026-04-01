package com.miempresa.comuniapp.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.miempresa.comuniapp.data.model.UserSession
import com.miempresa.comuniapp.domain.model.UserRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "session")

@Singleton
class SessionDataStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private object Keys {
        val USER_ID = stringPreferencesKey("user_id")
        val ROLE = stringPreferencesKey("role")
    }

    val sessionFlow: Flow<UserSession?> = context.dataStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID]
        val roleStr = prefs[Keys.ROLE]

        if (userId.isNullOrEmpty() || roleStr.isNullOrEmpty()) {
            null
        } else {
            try {
                UserSession(
                    userId = userId,
                    role = UserRole.valueOf(roleStr)
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun saveSession(userId: String, role: UserRole) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userId
            prefs[Keys.ROLE] = role.name
        }
    }

    suspend fun clearSession() {
        context.dataStore.edit { it.clear() }
    }
}
