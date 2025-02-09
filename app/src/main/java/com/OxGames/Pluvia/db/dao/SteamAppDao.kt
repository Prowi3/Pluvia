package com.OxGames.Pluvia.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.OxGames.Pluvia.data.SteamApp
import com.OxGames.Pluvia.enums.AppType
import com.OxGames.Pluvia.service.SteamService.Companion.INVALID_PKG_ID
import java.util.EnumSet
import kotlinx.coroutines.flow.Flow

@Dao
interface SteamAppDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(vararg app: SteamApp)

    @Update
    suspend fun update(app: SteamApp)

    @Query(
        "SELECT * FROM steam_app WHERE package_id != :invalidPkgId " +
            "AND owner_account_id = :ownerId AND type != 0 AND type & :filter = type",
    )
    fun getAllOwnedApps(
        ownerId: Int,
        filter: Int = AppType.code(EnumSet.allOf(AppType::class.java)),
        invalidPkgId: Int = INVALID_PKG_ID,
    ): Flow<List<SteamApp>>

    @Query("SELECT * FROM steam_app WHERE received_pics = 0")
    fun getAllAppsWithoutPICS(): Flow<List<SteamApp>>

    @Query("SELECT * FROM steam_app WHERE id = :appId")
    fun findApp(appId: Int): Flow<SteamApp?>

    @Query("DELETE from steam_app")
    suspend fun deleteAll()
}
