package com.example.bcs230117_foodmanager.data

import android.util.Log
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import retrofit2.Response

/**
 * Đồng bộ hai chiều giữa Room (local) và MockAPI. Mỗi lần chạy gồm các bước:
 *  1. linkExisting: ghép bản ghi local chưa có remoteId với bản ghi cùng tên đã có trên MockAPI
 *     (tránh tạo trùng khi cài lại app hoặc khi lần đẩy trước bị ngắt giữa chừng)
 *  2. đẩy các thay đổi local lên: xóa món, tạo/sửa loại, tạo/sửa món, xóa loại
 *  3. kéo dữ liệu từ MockAPI về; bản ghi local đang PENDING không bị ghi đè
 */
class SyncManager(private val db: AppDatabase, private val api: MockApi) {

    private val categoryDao = db.categoryDao()
    private val dishDao = db.dishDao()
    private val imageDao = db.imageDao()
    private val mutex = Mutex()

    /** Trả về true nếu đồng bộ xong không lỗi; lỗi mạng/API được bắt lại, app không crash. */
    suspend fun sync(): Boolean = mutex.withLock {
        try {
            linkExisting()
            pushDeletedDishes()
            pushCategories()
            pushDishes()
            pushDeletedCategories()
            pull()
            true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed: ${e::class.java.name}: ${e.message}")
            false
        }
    }

    // ---------- 1. Ghép bản ghi cùng tên ----------

    private suspend fun linkExisting() {
        val remoteCategories = fetchList { api.getCategories() }
        val remoteDishes = fetchList { api.getDishes() }

        val categories = categoryDao.getAllOnce()
        val usedCategoryIds = categories.mapNotNull { it.remoteId }.toMutableSet()
        categories.filter { it.remoteId == null && it.syncStatus != SyncStatus.DELETED }.forEach { local ->
            val match = remoteCategories.firstOrNull { r ->
                val id = r.id
                id != null && id !in usedCategoryIds && r.name.orEmpty().trim().equals(local.name, true)
            }
            val matchId = match?.id ?: return@forEach
            usedCategoryIds += matchId
            categoryDao.update(local.copy(remoteId = matchId)) // vẫn PENDING để đẩy dữ liệu local lên
        }

        val remoteIdOfCategory = categoryDao.getAllOnce().associate { it.id to it.remoteId }
        val dishes = dishDao.getAllOnce()
        val usedDishIds = dishes.mapNotNull { it.dish.remoteId }.toMutableSet()
        dishes.filter { it.dish.remoteId == null && it.dish.syncStatus != SyncStatus.DELETED }.forEach { item ->
            val categoryRemoteId = remoteIdOfCategory[item.dish.dishCategoryId] ?: return@forEach
            val match = remoteDishes.firstOrNull { r ->
                val id = r.id
                id != null && id !in usedDishIds && r.categoryId == categoryRemoteId &&
                        r.name.orEmpty().trim().equals(item.dish.name, true)
            }
            val matchId = match?.id ?: return@forEach
            usedDishIds += matchId
            dishDao.update(item.dish.copy(remoteId = matchId))
        }
    }

    // ---------- 2. Đẩy thay đổi local lên MockAPI ----------

    private suspend fun pushDeletedDishes() {
        dishDao.getAllOnce().filter { it.dish.syncStatus == SyncStatus.DELETED }.forEach { item ->
            item.dish.remoteId?.let { rid -> deleteRemote { api.deleteDish(rid) } }
            dishDao.delete(item.dish)
        }
    }

    private suspend fun pushCategories() {
        categoryDao.getAllOnce().filter { it.syncStatus == SyncStatus.PENDING }.forEach { local ->
            val body = CategoryDto(name = local.name, description = local.description)
            val rid = local.remoteId
            val saved = if (rid == null) {
                api.createCategory(body)
            } else try {
                api.updateCategory(rid, body)
            } catch (e: HttpException) {
                // Bản ghi đã bị xóa trên MockAPI: tạo lại
                if (e.code() == 404) api.createCategory(body) else throw e
            }
            val savedId = saved.id ?: throw IllegalStateException("MockAPI did not return an id")

            // Chỉ đánh dấu SYNCED nếu trong lúc đẩy người dùng chưa sửa tiếp bản ghi này
            val fresh = categoryDao.getById(local.id)
            if (fresh == null) {
                deleteRemote { api.deleteCategory(savedId) }
                return@forEach
            }
            categoryDao.update(
                fresh.copy(
                    remoteId = savedId,
                    syncStatus = if (fresh == local) SyncStatus.SYNCED else fresh.syncStatus
                )
            )
        }
    }

    private suspend fun pushDishes() {
        val remoteIdOfCategory = categoryDao.getAllOnce().associate { it.id to it.remoteId }
        dishDao.getAllOnce().filter { it.dish.syncStatus == SyncStatus.PENDING }.forEach { local ->
            // Loại của món chưa lên được MockAPI thì để lần sau
            val categoryRemoteId = remoteIdOfCategory[local.dish.dishCategoryId] ?: return@forEach
            val body = local.toDto(categoryRemoteId)
            val rid = local.dish.remoteId
            val saved = if (rid == null) {
                api.createDish(body)
            } else try {
                api.updateDish(rid, body)
            } catch (e: HttpException) {
                if (e.code() == 404) api.createDish(body) else throw e
            }
            val savedId = saved.id ?: throw IllegalStateException("MockAPI did not return an id")

            val fresh = dishDao.getById(local.dish.id)
            if (fresh == null) {
                deleteRemote { api.deleteDish(savedId) }
                return@forEach
            }
            dishDao.update(
                fresh.dish.copy(
                    remoteId = savedId,
                    syncStatus = if (fresh == local) SyncStatus.SYNCED else fresh.dish.syncStatus
                )
            )
        }
    }

    private suspend fun pushDeletedCategories() {
        categoryDao.getAllOnce().filter { it.syncStatus == SyncStatus.DELETED }.forEach { category ->
            category.remoteId?.let { rid -> deleteRemote { api.deleteCategory(rid) } }
            try {
                categoryDao.delete(category)
            } catch (e: Exception) {
                Log.w(TAG, "Category still referenced, will retry later", e)
            }
        }
    }

    // ---------- 3. Kéo dữ liệu từ MockAPI về ----------

    private suspend fun pull() {
        val remoteCategories = fetchList { api.getCategories() }
        val remoteDishes = fetchList { api.getDishes() }
        db.withTransaction {
            pullCategories(remoteCategories)
            pullDishes(remoteDishes)
            removeMissingCategories(remoteCategories)
        }
    }

    private suspend fun pullCategories(remote: List<CategoryDto>) {
        val byRemoteId = categoryDao.getAllOnce().filter { it.remoteId != null }.associateBy { it.remoteId!! }
        remote.forEach { r ->
            val rid = r.id ?: return@forEach
            val name = r.name.orEmpty().trim()
            if (name.isEmpty()) return@forEach
            val description = r.description.orEmpty()
            val local = byRemoteId[rid]
            if (local == null) {
                categoryDao.insert(
                    DishCategory(name = name, description = description, remoteId = rid, syncStatus = SyncStatus.SYNCED)
                )
            } else if (local.syncStatus == SyncStatus.SYNCED &&
                (local.name != name || local.description != description)
            ) {
                categoryDao.update(local.copy(name = name, description = description))
            }
        }
    }

    private suspend fun pullDishes(remote: List<DishDto>) {
        val localCategoryOfRemote = categoryDao.getAllOnce()
            .filter { it.remoteId != null }
            .associate { it.remoteId!! to it.id }
        val locals = dishDao.getAllOnce()
        val byRemoteId = locals.filter { it.dish.remoteId != null }.associateBy { it.dish.remoteId!! }
        val seenRemoteIds = HashSet<String>()

        remote.forEach { r ->
            val rid = r.id ?: return@forEach
            seenRemoteIds += rid
            val categoryRemoteId = r.categoryId ?: return@forEach
            val categoryId = localCategoryOfRemote[categoryRemoteId] ?: return@forEach
            val name = r.name.orEmpty().trim()
            if (name.isEmpty()) return@forEach
            val images = r.images.orEmpty().filter { !it.imageUrl.isNullOrBlank() }

            val local = byRemoteId[rid]
            if (local == null) {
                val newId = dishDao.insert(
                    Dish(
                        name = name,
                        price = r.price ?: 0.0,
                        preparationTime = r.preparationTime ?: 0,
                        isAvailable = r.isAvailable ?: true,
                        description = r.description.orEmpty(),
                        dishCategoryId = categoryId,
                        remoteId = rid,
                        syncStatus = SyncStatus.SYNCED
                    )
                )
                replaceImages(newId, images)
            } else if (local.dish.syncStatus == SyncStatus.SYNCED) {
                val updated = local.dish.copy(
                    name = name,
                    price = r.price ?: 0.0,
                    preparationTime = r.preparationTime ?: 0,
                    isAvailable = r.isAvailable ?: true,
                    description = r.description.orEmpty(),
                    dishCategoryId = categoryId
                )
                if (updated != local.dish) dishDao.update(updated)

                val localImages = local.images.map { it.imageUrl to it.isThumbnail }.toSet()
                val remoteImages = images.map { it.imageUrl.orEmpty() to (it.isThumbnail == true) }.toSet()
                if (localImages != remoteImages) replaceImages(local.dish.id, images)
            }
        }

        // Món đã đồng bộ mà không còn trên MockAPI -> đã bị xóa ở nơi khác. Bỏ qua khi danh sách rỗng để an toàn.
        if (remote.isNotEmpty()) {
            locals.filter {
                val rid = it.dish.remoteId
                rid != null && rid !in seenRemoteIds && it.dish.syncStatus == SyncStatus.SYNCED
            }.forEach { dishDao.delete(it.dish) }
        }
    }

    private suspend fun removeMissingCategories(remote: List<CategoryDto>) {
        if (remote.isEmpty()) return
        val remoteIds = remote.mapNotNull { it.id }.toSet()
        categoryDao.getAllOnce().filter {
            val rid = it.remoteId
            rid != null && rid !in remoteIds && it.syncStatus == SyncStatus.SYNCED
        }.forEach {
            try {
                categoryDao.delete(it)
            } catch (e: Exception) {
                // còn món tham chiếu: giữ lại
            }
        }
    }

    /** Thay toàn bộ ảnh của món bằng danh sách từ MockAPI, bảo đảm tối đa 1 ảnh đại diện. */
    private suspend fun replaceImages(dishId: Long, images: List<ImageDto>) {
        imageDao.deleteByDish(dishId)
        var thumbnailUsed = false
        images.forEach { img ->
            val isThumb = img.isThumbnail == true && !thumbnailUsed
            if (isThumb) thumbnailUsed = true
            imageDao.insert(DishImage(dishId = dishId, imageUrl = img.imageUrl.orEmpty(), isThumbnail = isThumb))
        }
        if (!thumbnailUsed) {
            imageDao.getByDish(dishId).firstOrNull()?.let { imageDao.setThumbnail(it.id) }
        }
    }

    // ---------- Tiện ích ----------

    private fun DishWithImages.toDto(categoryRemoteId: String) = DishDto(
        name = dish.name,
        price = dish.price,
        preparationTime = dish.preparationTime,
        isAvailable = dish.isAvailable,
        description = dish.description,
        categoryId = categoryRemoteId,
        images = images.map { ImageDto(imageUrl = it.imageUrl, isThumbnail = it.isThumbnail) }
    )

    /** MockAPI có thể trả 404 cho resource đang rỗng: coi như danh sách trống. */
    private suspend fun <T> fetchList(call: suspend () -> List<T>): List<T> =
        try {
            call()
        } catch (e: HttpException) {
            if (e.code() == 404) emptyList() else throw e
        }

    private suspend fun deleteRemote(call: suspend () -> Response<Unit>) {
        val response = call()
        if (!response.isSuccessful && response.code() != 404) throw HttpException(response)
    }

    private companion object {
        const val TAG = "BCS230117_Sync"
    }
}