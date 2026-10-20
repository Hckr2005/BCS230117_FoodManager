package com.example.bcs230117_foodmanager.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// Trong các câu lệnh SQL: SyncStatus 0 = SYNCED, 1 = PENDING, 2 = DELETED (xem object SyncStatus)

@Dao
interface DishCategoryDao {
    /** Không hiện loại đã xóa mềm (chờ đồng bộ xóa trên MockAPI). */
    @Query("SELECT * FROM BCS230117_DishCategory WHERE BCS230117_SyncStatus != 2 ORDER BY BCS230117_Name")
    fun observeAll(): Flow<List<DishCategory>>

    @Query("SELECT * FROM BCS230117_DishCategory")
    suspend fun getAllOnce(): List<DishCategory>

    @Query("SELECT * FROM BCS230117_DishCategory WHERE BCS230117_Id = :id")
    suspend fun getById(id: Long): DishCategory?

    @Query(
        """SELECT COUNT(*) FROM BCS230117_Dish
           WHERE BCS230117_DishCategoryId = :categoryId AND BCS230117_SyncStatus != 2"""
    )
    suspend fun countDishes(categoryId: Long): Int

    /** Số loại món có thay đổi chưa đồng bộ. */
    @Query("SELECT COUNT(*) FROM BCS230117_DishCategory WHERE BCS230117_SyncStatus != 0")
    fun observePendingCount(): Flow<Int>

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

    /** Đếm món trùng tên (không phân biệt hoa thường) trong cùng một loại; bỏ qua món đã xóa mềm. */
    @Query(
        """SELECT COUNT(*) FROM BCS230117_Dish
           WHERE BCS230117_DishCategoryId = :categoryId
           AND LOWER(BCS230117_Name) = LOWER(:name)
           AND BCS230117_Id != :excludeId
           AND BCS230117_SyncStatus != 2"""
    )
    suspend fun countSameName(categoryId: Long, name: String, excludeId: Long): Int

    /** Số món có thay đổi chưa đồng bộ. */
    @Query("SELECT COUNT(*) FROM BCS230117_Dish WHERE BCS230117_SyncStatus != 0")
    fun observePendingCount(): Flow<Int>

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

    @Query("DELETE FROM BCS230117_DishImage WHERE BCS230117_DishId = :dishId")
    suspend fun deleteByDish(dishId: Long)

    @Query("UPDATE BCS230117_DishImage SET BCS230117_IsThumbnail = 0 WHERE BCS230117_DishId = :dishId")
    suspend fun clearThumbnail(dishId: Long)

    @Query("UPDATE BCS230117_DishImage SET BCS230117_IsThumbnail = 1 WHERE BCS230117_Id = :imageId")
    suspend fun setThumbnail(imageId: Long)
}