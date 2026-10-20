package com.example.bcs230117_foodmanager.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bcs230117_foodmanager.data.AppDatabase
import com.example.bcs230117_foodmanager.data.DishCategory
import com.example.bcs230117_foodmanager.data.FoodRepository
import com.example.bcs230117_foodmanager.data.RepoResult
import com.example.bcs230117_foodmanager.data.SyncStatus
import com.example.bcs230117_foodmanager.data.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryItem(val category: DishCategory, val dishCount: Int)

class CategoryViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FoodRepository(AppDatabase.getInstance(application))

    /** Mỗi loại kèm số món đang thuộc loại đó. */
    val items: StateFlow<List<CategoryItem>> = combine(
        repo.observeCategories().catch { emit(emptyList()) },
        repo.observeDishes().catch { emit(emptyList()) }
    ) { categories, dishes ->
        val visible = dishes.filter { it.dish.syncStatus != SyncStatus.DELETED }
        categories.map { c ->
            CategoryItem(c, visible.count { it.dish.dishCategoryId == c.id })
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _error = MutableStateFlow<ValidationError?>(null)
    val error: StateFlow<ValidationError?> = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    /** id = 0: thêm mới; id > 0: sửa. Lỗi được trả về cho hộp thoại. */
    fun save(
        id: Long,
        name: String,
        description: String,
        onSuccess: () -> Unit,
        onError: (ValidationError) -> Unit
    ) {
        viewModelScope.launch {
            val existing = items.value.firstOrNull { it.category.id == id }?.category
            val category = DishCategory(
                id = id,
                name = name,
                description = description.trim(),
                remoteId = existing?.remoteId
            )
            when (val result = repo.saveCategory(category)) {
                is RepoResult.Success -> onSuccess()
                is RepoResult.Failure -> onError(result.errors.firstOrNull() ?: ValidationError.UNKNOWN)
            }
        }
    }

    /** Loại đang có món thì Repository trả về CATEGORY_IN_USE và không xóa. */
    fun delete(id: Long) {
        viewModelScope.launch {
            val result = repo.deleteCategory(id)
            if (result is RepoResult.Failure) {
                _error.value = result.errors.firstOrNull() ?: ValidationError.UNKNOWN
            }
        }
    }
}