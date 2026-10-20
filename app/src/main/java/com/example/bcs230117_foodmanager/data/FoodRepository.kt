package com.example.bcs230117_foodmanager.data

import android.util.Patterns
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

/** Các mã lỗi nghiệp vụ. Giao diện sẽ dịch sang chữ ở Bước 8. */
enum class ValidationError {
    NAME_EMPTY,
    PRICE_INVALID,
    PREPARATION_TIME_INVALID,
    CATEGORY_INVALID,
    NAME_DUPLICATE,
    NOT_FOUND,
    CATEGORY_IN_USE,
    IMAGE_URL_INVALID,
    UNKNOWN
}

/** Kết quả của một thao tác: thành công (kèm giá trị) hoặc thất bại (kèm danh sách lỗi). */
sealed class RepoResult<out T> {
    data class Success<T>(val value: T) : RepoResult<T>()
    data class Failure(val errors: List<ValidationError>) : RepoResult<Nothing>()
}

class FoodRepository(private val db: AppDatabase) {

    private val categoryDao = db.categoryDao()
    private val dishDao = db.dishDao()
    private val imageDao = db.imageDao()

    private fun fail(vararg errors: ValidationError) = RepoResult.Failure(errors.toList())

    // ---------- Đọc dữ liệu ----------

    fun observeCategories(): Flow<List<DishCategory>> = categoryDao.observeAll()

    fun observeDishes(): Flow<List<DishWithImages>> = dishDao.observeAll()

    /** Id không tồn tại -> trả về lỗi NOT_FOUND thay vì crash. */
    suspend fun getDish(id: Long): RepoResult<DishWithImages> {
        val dish = dishDao.getById(id) ?: return fail(ValidationError.NOT_FOUND)
        return RepoResult.Success(dish)
    }

    // ---------- Loại món ----------

    suspend fun saveCategory(category: DishCategory): RepoResult<Long> {
        val name = category.name.trim()
        if (name.isEmpty()) return fail(ValidationError.NAME_EMPTY)
        return try {
            if (category.id == 0L) {
                RepoResult.Success(
                    categoryDao.insert(category.copy(name = name, syncStatus = SyncStatus.PENDING))
                )
            } else {
                if (categoryDao.getById(category.id) == null) return fail(ValidationError.NOT_FOUND)
                categoryDao.update(category.copy(name = name, syncStatus = SyncStatus.PENDING))
                RepoResult.Success(category.id)
            }
        } catch (e: Exception) {
            fail(ValidationError.UNKNOWN)
        }
    }

    /** Không cho xóa loại đang có món. */
    suspend fun deleteCategory(categoryId: Long): RepoResult<Unit> {
        val category = categoryDao.getById(categoryId) ?: return fail(ValidationError.NOT_FOUND)
        if (categoryDao.countDishes(categoryId) > 0) return fail(ValidationError.CATEGORY_IN_USE)
        return try {
            categoryDao.delete(category)
            RepoResult.Success(Unit)
        } catch (e: Exception) {
            // Ràng buộc khóa ngoại RESTRICT là lớp bảo vệ thứ hai
            fail(ValidationError.CATEGORY_IN_USE)
        }
    }

    // ---------- Món ăn ----------

    /** Thêm mới (id = 0) hoặc cập nhật (id > 0). Trả về id của món. */
    suspend fun saveDish(dish: Dish): RepoResult<Long> {
        val errors = mutableListOf<ValidationError>()
        val name = dish.name.trim()

        if (name.isEmpty()) errors += ValidationError.NAME_EMPTY
        if (dish.price.isNaN() || dish.price.isInfinite() || dish.price <= 0.0) {
            errors += ValidationError.PRICE_INVALID
        }
        if (dish.preparationTime <= 0) errors += ValidationError.PREPARATION_TIME_INVALID

        if (categoryDao.getById(dish.dishCategoryId) == null) {
            errors += ValidationError.CATEGORY_INVALID
        } else if (name.isNotEmpty() &&
            dishDao.countSameName(dish.dishCategoryId, name, dish.id) > 0
        ) {
            errors += ValidationError.NAME_DUPLICATE
        }

        if (dish.id != 0L && dishDao.getById(dish.id) == null) {
            errors += ValidationError.NOT_FOUND
        }
        if (errors.isNotEmpty()) return RepoResult.Failure(errors)

        // Mọi thay đổi đều đánh dấu PENDING để Bước 9 đồng bộ lên MockAPI
        val toSave = dish.copy(name = name, syncStatus = SyncStatus.PENDING)
        return try {
            if (dish.id == 0L) {
                RepoResult.Success(dishDao.insert(toSave))
            } else {
                dishDao.update(toSave)
                RepoResult.Success(dish.id)
            }
        } catch (e: Exception) {
            fail(ValidationError.UNKNOWN)
        }
    }

    /** Xóa món; ảnh của món tự bị xóa theo (CASCADE). */
    suspend fun deleteDish(dishId: Long): RepoResult<Unit> {
        val dish = dishDao.getById(dishId) ?: return fail(ValidationError.NOT_FOUND)
        return try {
            dishDao.delete(dish.dish)
            RepoResult.Success(Unit)
        } catch (e: Exception) {
            fail(ValidationError.UNKNOWN)
        }
    }

    // ---------- Ảnh ----------

    /** Thêm ảnh bằng ImageUrl. Ảnh đầu tiên của món tự thành ảnh đại diện. */
    suspend fun addImage(dishId: Long, imageUrl: String): RepoResult<Long> {
        val url = imageUrl.trim()
        val validUrl = (url.startsWith("http://") || url.startsWith("https://")) &&
                Patterns.WEB_URL.matcher(url).matches()
        if (!validUrl) return fail(ValidationError.IMAGE_URL_INVALID)

        val dish = dishDao.getById(dishId) ?: return fail(ValidationError.NOT_FOUND)
        return try {
            db.withTransaction {
                val id = imageDao.insert(
                    DishImage(
                        dishId = dishId,
                        imageUrl = url,
                        isThumbnail = dish.images.isEmpty()
                    )
                )
                markPending(dishId)
                RepoResult.Success(id)
            }
        } catch (e: Exception) {
            fail(ValidationError.UNKNOWN)
        }
    }

    /** Đổi ảnh đại diện: bỏ cờ ở mọi ảnh của món, rồi bật cờ cho ảnh được chọn (tối đa 1). */
    suspend fun setThumbnail(dishId: Long, imageId: Long): RepoResult<Unit> {
        val images = imageDao.getByDish(dishId)
        if (images.none { it.id == imageId }) return fail(ValidationError.NOT_FOUND)
        return try {
            db.withTransaction {
                imageDao.clearThumbnail(dishId)
                imageDao.setThumbnail(imageId)
                markPending(dishId)
                RepoResult.Success(Unit)
            }
        } catch (e: Exception) {
            fail(ValidationError.UNKNOWN)
        }
    }

    /** Xóa ảnh; nếu xóa ảnh đại diện thì chuyển ảnh còn lại đầu tiên làm ảnh đại diện. */
    suspend fun deleteImage(dishId: Long, imageId: Long): RepoResult<Unit> {
        val images = imageDao.getByDish(dishId)
        val target = images.firstOrNull { it.id == imageId } ?: return fail(ValidationError.NOT_FOUND)
        return try {
            db.withTransaction {
                imageDao.delete(target)
                if (target.isThumbnail) {
                    images.firstOrNull { it.id != imageId }?.let { imageDao.setThumbnail(it.id) }
                }
                markPending(dishId)
                RepoResult.Success(Unit)
            }
        } catch (e: Exception) {
            fail(ValidationError.UNKNOWN)
        }
    }

    /** Ảnh thay đổi cũng coi như món thay đổi, để lần đồng bộ sau đẩy lên MockAPI. */
    private suspend fun markPending(dishId: Long) {
        dishDao.getById(dishId)?.let {
            dishDao.update(it.dish.copy(syncStatus = SyncStatus.PENDING))
        }
    }
}