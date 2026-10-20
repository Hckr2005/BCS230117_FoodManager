package com.example.bcs230117_foodmanager.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.bcs230117_foodmanager.data.AppDatabase
import com.example.bcs230117_foodmanager.data.Dish
import com.example.bcs230117_foodmanager.data.DishCategory
import com.example.bcs230117_foodmanager.data.FoodRepository
import com.example.bcs230117_foodmanager.data.RepoResult
import com.example.bcs230117_foodmanager.data.ValidationError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Các ô nhập đều là chuỗi để người dùng có thể để trống / nhập sai; kiểm tra ở Repository. */
data class DishFormState(
    val isEdit: Boolean = false,
    val loaded: Boolean = false,
    val notFound: Boolean = false,
    val saving: Boolean = false,
    val name: String = "",
    val price: String = "",
    val preparationTime: String = "",
    val isAvailable: Boolean = true,
    val description: String = "",
    val categoryId: Long? = null,
    val errors: Set<ValidationError> = emptySet()
)

class DishFormViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repo = FoodRepository(AppDatabase.getInstance(application))

    // dishId = 0 nghĩa là thêm mới; > 0 là sửa món có id đó
    private val dishId: Long = savedStateHandle.get<Long>("dishId") ?: 0L
    private var existing: Dish? = null

    private val _form = MutableStateFlow(DishFormState(isEdit = dishId != 0L, loaded = dishId == 0L))
    val form: StateFlow<DishFormState> = _form.asStateFlow()

    // Danh sách loại món lấy từ database cho ô chọn loại
    val categories: StateFlow<List<DishCategory>> = repo.observeCategories()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    init {
        if (dishId != 0L) {
            viewModelScope.launch {
                when (val result = repo.getDish(dishId)) {
                    is RepoResult.Success -> {
                        val d = result.value.dish
                        existing = d
                        _form.update {
                            it.copy(
                                loaded = true,
                                name = d.name,
                                price = d.price.toLong().toString(),
                                preparationTime = d.preparationTime.toString(),
                                isAvailable = d.isAvailable,
                                description = d.description,
                                categoryId = d.dishCategoryId
                            )
                        }
                    }
                    is RepoResult.Failure -> _form.update { it.copy(loaded = true, notFound = true) }
                }
            }
        }
    }

    fun onNameChange(v: String) = _form.update {
        it.copy(name = v, errors = it.errors - ValidationError.NAME_EMPTY - ValidationError.NAME_DUPLICATE)
    }

    fun onPriceChange(v: String) = _form.update {
        it.copy(price = v.filter(Char::isDigit).take(10), errors = it.errors - ValidationError.PRICE_INVALID)
    }

    fun onTimeChange(v: String) = _form.update {
        it.copy(
            preparationTime = v.filter(Char::isDigit).take(4),
            errors = it.errors - ValidationError.PREPARATION_TIME_INVALID
        )
    }

    fun onAvailableChange(v: Boolean) = _form.update { it.copy(isAvailable = v) }

    fun onDescriptionChange(v: String) = _form.update { it.copy(description = v) }

    fun onCategoryChange(id: Long) = _form.update {
        it.copy(
            categoryId = id,
            errors = it.errors - ValidationError.CATEGORY_INVALID - ValidationError.NAME_DUPLICATE
        )
    }

    fun save(onSaved: () -> Unit) {
        val f = _form.value
        if (f.saving) return
        _form.update { it.copy(saving = true, errors = emptySet()) }

        viewModelScope.launch {
            val dish = Dish(
                id = existing?.id ?: 0L,
                name = f.name,
                price = f.price.toDoubleOrNull() ?: 0.0,
                preparationTime = f.preparationTime.toIntOrNull() ?: 0,
                isAvailable = f.isAvailable,
                description = f.description.trim(),
                dishCategoryId = f.categoryId ?: -1L,
                remoteId = existing?.remoteId
            )
            when (val result = repo.saveDish(dish)) {
                is RepoResult.Success -> {
                    _form.update { it.copy(saving = false) }
                    onSaved()
                }
                is RepoResult.Failure ->
                    _form.update { it.copy(saving = false, errors = result.errors.toSet()) }
            }
        }
    }
}