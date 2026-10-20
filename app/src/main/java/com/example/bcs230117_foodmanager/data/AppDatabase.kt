package com.example.bcs230117_foodmanager.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

// ===== ẢNH MẪU (Wikimedia Commons) =====
private const val WIKI = "https://commons.wikimedia.org/wiki/Special:FilePath/"

private const val IMG_PHO_1 = WIKI + "Pho-Beef-Noodles-2008.jpg?width=500"
private const val IMG_PHO_2 = WIKI + "Ph%E1%BB%9F%20b%C3%B2%20(39425047901).jpg?width=500"

private const val IMG_BUN_CHA =
    "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b7/B%C3%BAn_ch%E1%BA%A3_H%C3%A0_N%E1%BB%99i_%28th%E1%BB%8Bt_n%C6%B0%E1%BB%9Bng%29_t%E1%BA%A1i_Nguy%E1%BB%85n_S%C6%A1n%2C_T%C3%A2n_Ph%C3%BA_%282%29.jpg/960px-B%C3%BAn_ch%E1%BA%A3_H%C3%A0_N%E1%BB%99i_%28th%E1%BB%8Bt_n%C6%B0%E1%BB%9Bng%29_t%E1%BA%A1i_Nguy%E1%BB%85n_S%C6%A1n%2C_T%C3%A2n_Ph%C3%BA_%282%29.jpg"

private const val IMG_COM_TAM =
    "https://thumb.wikimedia.org/wikipedia/commons/thumb/b/b0/C%C6%A1m_T%E1%BA%A5m%2C_Da_Nang%2C_Vietnam.jpg/960px-C%C6%A1m_T%E1%BA%A5m%2C_Da_Nang%2C_Vietnam.jpg"

private const val IMG_TRA_DAO =
    "https://thumb.wikimedia.org/wikipedia/commons/thumb/6/6b/Peach_iced_tea_with_orange_slices.jpg/960px-Peach_iced_tea_with_orange_slices.jpg"

// Chè ba màu chưa có ảnh: để trống, danh sách sẽ hiện biểu tượng món ăn làm placeholder
private const val IMG_CHE = ""

@Database(
    entities = [DishCategory::class, Dish::class, DishImage::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): DishCategoryDao
    abstract fun dishDao(): DishDao
    abstract fun imageDao(): DishImageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "BCS230117_FoodManager.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            seed(db)
                        }
                    })
                    .build()
                    .also { INSTANCE = it }
            }

        private fun seed(db: SupportSQLiteDatabase) {
            val categoryCols =
                "BCS230117_Id, BCS230117_Name, BCS230117_Description, BCS230117_RemoteId, BCS230117_SyncStatus"
            val dishCols =
                "BCS230117_Id, BCS230117_Name, BCS230117_Price, BCS230117_PreparationTime, " +
                        "BCS230117_IsAvailable, BCS230117_Description, BCS230117_DishCategoryId, " +
                        "BCS230117_RemoteId, BCS230117_SyncStatus"

            val statements = listOf(
                // 3 loại món
                "INSERT INTO BCS230117_DishCategory ($categoryCols) VALUES (1, 'Main Course', 'Rice, noodle and soup dishes', NULL, 1)",
                "INSERT INTO BCS230117_DishCategory ($categoryCols) VALUES (2, 'Drinks', 'Hot and cold beverages', NULL, 1)",
                "INSERT INTO BCS230117_DishCategory ($categoryCols) VALUES (3, 'Dessert', 'Sweet dishes', NULL, 1)",
                // 5 món
                "INSERT INTO BCS230117_Dish ($dishCols) VALUES (1, 'Pho Bo', 45000, 20, 1, 'Beef noodle soup', 1, NULL, 1)",
                "INSERT INTO BCS230117_Dish ($dishCols) VALUES (2, 'Bun Cha', 40000, 25, 1, 'Grilled pork with vermicelli', 1, NULL, 1)",
                "INSERT INTO BCS230117_Dish ($dishCols) VALUES (3, 'Com Tam', 35000, 15, 1, 'Broken rice with grilled pork', 1, NULL, 1)",
                "INSERT INTO BCS230117_Dish ($dishCols) VALUES (4, 'Tra Dao', 25000, 5, 1, 'Peach tea', 2, NULL, 1)",
                "INSERT INTO BCS230117_Dish ($dishCols) VALUES (5, 'Che Ba Mau', 30000, 10, 0, 'Three-color dessert', 3, NULL, 1)"
            )
            statements.forEach { db.execSQL(it) }

            // Ảnh: (dishId, url, isThumbnail). Dòng nào url trống thì bỏ qua.
            listOf(
                Triple(1, IMG_PHO_1, 1),
                Triple(1, IMG_PHO_2, 0),
                Triple(2, IMG_BUN_CHA, 1),
                Triple(3, IMG_COM_TAM, 1),
                Triple(4, IMG_TRA_DAO, 1),
                Triple(5, IMG_CHE, 1)
            ).filter { it.second.isNotBlank() }
                .forEach { (dishId, url, thumb) ->
                    db.execSQL(
                        "INSERT INTO BCS230117_DishImage " +
                                "(BCS230117_DishId, BCS230117_ImageUrl, BCS230117_IsThumbnail) " +
                                "VALUES ($dishId, '$url', $thumb)"
                    )
                }
        }
    }
}