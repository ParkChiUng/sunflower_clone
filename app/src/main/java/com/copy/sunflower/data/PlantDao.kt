package com.copy.sunflower.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao                        // Room 데이터베이스에 액세스할 수 있는 DAO(Data Access Object) 인터페이스를 정의
interface PlantDao {
    @Query("SELECT * FROM plants ORDER BY name")
    fun getPlants(): Flow<List<Plant>>  // flow : 비동기 데이터 스트림을 리스트 형식으로 plant에서 가져옴

    @Query("SELECT * FROM plants WHERE growZoneNumber = :growZoneNumber ORDER BY name")
    fun getPlantsWithGrowZoneNumber(growZoneNumber: Int): Flow<List<Plant>>

    @Query("SELECT * FROM plants WHERE id = :plantId")
    fun getPlant(plantId: String): Flow<Plant>

    @Upsert
    suspend fun upsertAll(plants: List<Plant>)
}