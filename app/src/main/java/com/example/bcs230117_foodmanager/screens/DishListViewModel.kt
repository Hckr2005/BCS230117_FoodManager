package com.example.bcs230117_foodmanager.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bcs230117_foodmanager.data.AppDatabase
import com.example.bcs230117_foodmanager.data.DishCategory
import com.example.bcs230117_foodmanager.data.DishWithImages
import com.example.bcs230117_foodmanager.data.FoodRepository
import com.example.bcs230117_foodmanager.data.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class SortOption { NONE, PRICE_ASC, PRICE_DESC, TIME_ASC }

/** Toàn bộ điều kiện tìm kiếm/lọc/sắp xếp. Giá lưu dạng chuỗi để ô nhập có thể để trống. */
data class DishFilter(
    val query: String = "",
    val categoryId: Long? = null,
    val onlyAvailable: Boolean = false,
    val minPrice: String = "",
    val maxPrice: String = "",
    val sort: SortOption = SortOption.NONE
) {
    val minValue: Double? get() = minPrice.toDoubleOrNull()
    val maxValue: Double? get() = maxPrice.toDoubleOrNull()

    /** Khoảng giá không hợp lệ khi giá nhỏ nhất lớn hơn giá lớn nhất. */
    val priceRangeInvalid: Boolean
        get() {
            val min = minValue
            val max = maxValue
            return min != null && max != null && min > max
        }
}

data class DishListUiState(
    val dishes: List<DishWithImages> = emptyList(),
    val categories: List<DishCategory> = emptyList(),
    val isLoading: Boolean = true,
    val hasAnyDish: Boolean = false
)

class DishListViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = FoodRepository(AppDatabase.getInstance(application))

    private val _filter = MutableStateFlow(DishFilter())
    val filter: StateFlow<DishFilter> = _filter.asStateFlow()

    val uiState: StateFlow<DishListUiState> = combine(
        repo.observeDishes().catch { emit(emptyList()) },
        repo.observeCategories().catch { emit(emptyList()) },
        _filter
    ) { dishes, categories, filter ->
        // Món đã xóa local và đang chờ xóa trên MockAPI (Bước 9) thì không hiển thị
        val visible = dishes.filter { it.dish.syncStatus != SyncStatus.DELETED }
        DishListUiState(
            dishes = applyFilter(visible, filter),
            categories = categories,
            isLoading = false,
            hasAnyDish = visible.isNotEmpty()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DishListUiState())

    private fun applyFilter(list: List<DishWithImages>, f: DishFilter): List<DishWithImages> {
        val query = f.query.trim()
        val min = f.minValue
        val max = f.maxValue
        val rangeOk = !f.priceRangeInvalid // khoảng giá sai thì bỏ qua điều kiện giá

        val filtered = list.filter { item ->
            val d = item.dish
            (query.isEmpty() || d.name.contains(query, ignoreCase = true)) &&
                    (f.categoryId == null || d.dishCategoryId == f.categoryId) &&
                    (!f.onlyAvailable || d.isAvailable) &&
                    (!rangeOk || ((min == null || d.price >= min) && (max == null || d.price <= max)))
        }
        return when (f.sort) {
            SortOption.NONE -> filtered.sortedBy { it.dish.id }
            SortOption.PRICE_ASC -> filtered.sortedBy { it.dish.price }
            SortOption.PRICE_DESC -> filtered.sortedByDescending { it.dish.price }
            SortOption.TIME_ASC -> filtered.sortedBy { it.dish.preparationTime }
        }
    }

    fun onQueryChange(value: String) = _filter.update { it.copy(query = value) }
    fun onCategorySelected(id: Long?) = _filter.update { it.copy(categoryId = id) }
    fun onOnlyAvailableChange(value: Boolean) = _filter.update { it.copy(onlyAvailable = value) }
    fun onMinPriceChange(value: String) = _filter.update { it.copy(minPrice = value.filter(Char::isDigit)) }
    fun onMaxPriceChange(value: String) = _filter.update { it.copy(maxPrice = value.filter(Char::isDigit)) }
    fun onSortChange(option: SortOption) = _filter.update { it.copy(sort = option) }
    fun onClearFilters() = _filter.update { DishFilter(sort = it.sort) }
}