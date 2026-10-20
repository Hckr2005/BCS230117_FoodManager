package com.example.bcs230117_foodmanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DishCategoryDao {
    @Query("SELECT * FROM BCS230117_DishCategory ORDER BY BCS230117_Name")
    fun observeAll(): Flow<List<DishCategory>>

    @Query("SELECT * FROM BCS230117_DishCategory WHERE BCS230117_Id = :id")
    suspend fun getById(id: Long): DishCategory?

    @Query("SELECT COUNT(*) FROM BCS230117_Dish WHERE BCS230117_DishCategoryId = :categoryId")
    suspend fun countDishes(categoryId: Long): Int

    @Insert
    suspend fun insert(category: DishCategory): Long

    @Update
    suspend fun update(category: DishCategory)

    @Delete
    suspend fun delete(category: DishCategory)
}

@Dao
interface DishDao {
    @Transaction
    @Query("SELECT * FROM BCS230117_Dish")
    fun observeAll(): Flow<List<DishWithImages>>

    @Transaction
    @Query("SELECT * FROM BCS230117_Dish")
    suspend fun getAllOnce(): List<DishWithImages>

    @Transaction
    @Query("SELECT * FROM BCS230117_Dish WHERE BCS230117_Id = :id")
    suspend fun getById(id: Long): DishWithImages?

    /** Đếm món trùng tên (không phân biệt hoa thường) trong cùng một loại. */
    @Query(
        """SELECT COUNT(*) FROM BCS230117_Dish
           WHERE BCS230117_DishCategoryId = :categoryId
           AND LOWER(BCS230117_Name) = LOWER(:name)
           AND BCS230117_Id != :excludeId"""
    )
    suspend fun countSameName(categoryId: Long, name: String, excludeId: Long): Int

    @Insert
    suspend fun insert(dish: Dish): Long

    @Update
    suspend fun update(dish: Dish)

    @Delete
    suspend fun delete(dish: Dish)
}

@Dao
interface DishImageDao {
    @Query("SELECT * FROM BCS230117_DishImage WHERE BCS230117_DishId = :dishId")
    suspend fun getByDish(dishId: Long): List<DishImage>

    @Insert
    suspend fun insert(image: DishImage): Long

    @Delete
    suspend fun delete(image: DishImage)

    @Query("UPDATE BCS230117_DishImage SET BCS230117_IsThumbnail = 0 WHERE BCS230117_DishId = :dishId")
    suspend fun clearThumbnail(dishId: Long)

    @Query("UPDATE BCS230117_DishImage SET BCS230117_IsThumbnail = 1 WHERE BCS230117_Id = :imageId")
    suspend fun setThumbnail(imageId: Long)
}