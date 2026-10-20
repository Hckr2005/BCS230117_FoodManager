package com.example.bcs230117_foodmanager.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.bcs230117_foodmanager.data.AppDatabase
import com.example.bcs230117_foodmanager.data.DishWithImages
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

data class DishDetailUiState(
    val isLoading: Boolean = true,
    val dish: DishWithImages? = null,
    val categoryName: String = "",
    val notFound: Boolean = false
)

class DishDetailViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repo = FoodRepository(AppDatabase.getInstance(application))

    // Id lấy từ tham số điều hướng; không có thì coi như không tồn tại
    private val dishId: Long = savedStateHandle.get<Long>("dishId") ?: -1L

    val uiState: StateFlow<DishDetailUiState> = combine(
        repo.observeDishes().catch { emit(emptyList()) },
        repo.observeCategories().catch { emit(emptyList()) }
    ) { dishes, categories ->
        val found = dishes.firstOrNull {
            it.dish.id == dishId && it.dish.syncStatus != SyncStatus.DELETED
        }
        DishDetailUiState(
            isLoading = false,
            dish = found,
            categoryName = categories.firstOrNull { it.id == found?.dish?.dishCategoryId }?.name.orEmpty(),
            notFound = found == null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DishDetailUiState())

    // Lỗi của các thao tác (xóa món, đổi ảnh đại diện, xóa ảnh) hiện bằng Snackbar
    private val _actionError = MutableStateFlow<ValidationError?>(null)
    val actionError: StateFlow<ValidationError?> = _actionError.asStateFlow()

    fun clearError() {
        _actionError.value = null
    }

    private fun report(result: RepoResult<*>) {
        if (result is RepoResult.Failure) {
            _actionError.value = result.errors.firstOrNull() ?: ValidationError.UNKNOWN
        }
    }

    fun delete(onDone: () -> Unit) {
        viewModelScope.launch {
            when (val result = repo.deleteDish(dishId)) {
                is RepoResult.Success -> onDone()
                is RepoResult.Failure -> report(result)
            }
        }
    }

    /** Thêm ảnh bằng ImageUrl. Lỗi (URL sai...) trả về cho hộp thoại hiển thị. */
    fun addImage(url: String, onSuccess: () -> Unit, onError: (ValidationError) -> Unit) {
        viewModelScope.launch {
            when (val result = repo.addImage(dishId, url)) {
                is RepoResult.Success -> onSuccess()
                is RepoResult.Failure -> onError(result.errors.firstOrNull() ?: ValidationError.UNKNOWN)
            }
        }
    }

    fun setThumbnail(imageId: Long) {
        viewModelScope.launch { report(repo.setThumbnail(dishId, imageId)) }
    }

    fun deleteImage(imageId: Long) {
        viewModelScope.launch { report(repo.deleteImage(dishId, imageId)) }
    }
}