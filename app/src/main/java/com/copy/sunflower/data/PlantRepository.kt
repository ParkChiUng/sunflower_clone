package com.copy.sunflower.data

import javax.inject.Inject
import javax.inject.Singleton

@Singleton // 싱글톤 객체
class PlantRepository @Inject constructor(private val plantDao: PlantDao) { // 생성자에서 PlantDao 의존성 주입

    fun getPlants() = plantDao.getPlants()

    fun getPlant(plantId: String) = plantDao.getPlant(plantId)

    fun getPlantsWithGrowZoneNumber(growZoneNumber: Int) =
        plantDao.getPlantsWithGrowZoneNumber(growZoneNumber)

    companion object {

        @Volatile private var instance: PlantRepository? = null // 스레드 가시성 보장

        fun getInstance(plantDao: PlantDao) =                   // 객체의 인스턴스를 생성하거나 이미 생성된 인스턴스 반환
            instance ?: synchronized(this) {
                instance ?: PlantRepository(plantDao).also { instance = it }
            }
    }
}
