package com.example.bcs230117_foodmanager.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Trạng thái đồng bộ với MockAPI (dùng ở Bước 9). */
object SyncStatus {
    const val SYNCED = 0   // đã khớp với MockAPI
    const val PENDING = 1  // tạo/sửa khi offline, chờ đẩy lên
    const val DELETED = 2  // đã xóa local, chờ xóa trên MockAPI
}

@Entity(tableName = "BCS230117_DishCategory")
data class DishCategory(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "BCS230117_Id") val id: Long = 0,
    @ColumnInfo(name = "BCS230117_Name") val name: String,
    @ColumnInfo(name = "BCS230117_Description") val description: String = "",
    @ColumnInfo(name = "BCS230117_RemoteId") val remoteId: String? = null,
    @ColumnInfo(name = "BCS230117_SyncStatus") val syncStatus: Int = SyncStatus.PENDING
)

@Entity(
    tableName = "BCS230117_Dish",
    foreignKeys = [
        ForeignKey(
            entity = DishCategory::class,
            parentColumns = ["BCS230117_Id"],
            childColumns = ["BCS230117_DishCategoryId"],
            onDelete = ForeignKey.RESTRICT // không cho xóa loại đang có món
        )
    ],
    indices = [Index("BCS230117_DishCategoryId")]
)
data class Dish(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "BCS230117_Id") val id: Long = 0,
    @ColumnInfo(name = "BCS230117_Name") val name: String,
    @ColumnInfo(name = "BCS230117_Price") val price: Double,
    @ColumnInfo(name = "BCS230117_PreparationTime") val preparationTime: Int, // phút
    @ColumnInfo(name = "BCS230117_IsAvailable") val isAvailable: Boolean = true,
    @ColumnInfo(name = "BCS230117_Description") val description: String = "",
    @ColumnInfo(name = "BCS230117_DishCategoryId") val dishCategoryId: Long,
    @ColumnInfo(name = "BCS230117_RemoteId") val remoteId: String? = null,
    @ColumnInfo(name = "BCS230117_SyncStatus") val syncStatus: Int = SyncStatus.PENDING
)

@Entity(
    tableName = "BCS230117_DishImage",
    foreignKeys = [
        ForeignKey(
            entity = Dish::class,
            parentColumns = ["BCS230117_Id"],
            childColumns = ["BCS230117_DishId"],
            onDelete = ForeignKey.CASCADE // xóa món thì xóa luôn ảnh
        )
    ],
    indices = [Index("BCS230117_DishId")]
)
data class DishImage(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "BCS230117_Id") val id: Long = 0,
    @ColumnInfo(name = "BCS230117_DishId") val dishId: Long,
    @ColumnInfo(name = "BCS230117_ImageUrl") val imageUrl: String,
    @ColumnInfo(name = "BCS230117_IsThumbnail") val isThumbnail: Boolean = false
)

/** Một món kèm danh sách ảnh (quan hệ 1-nhiều). */
data class DishWithImages(
    @Embedded val dish: Dish,
    @Relation(
        parentColumn = "BCS230117_Id",
        entityColumn = "BCS230117_DishId"
    )
    val images: List<DishImage>
)