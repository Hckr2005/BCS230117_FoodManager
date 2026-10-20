package com.example.bcs230117_foodmanager.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bcs230117_foodmanager.R
import com.example.bcs230117_foodmanager.data.DishCategory
import com.example.bcs230117_foodmanager.data.DishWithImages
import java.util.Locale

/** Thời gian chế biến ≤ 15 phút là "Quick", ngược lại là "Regular" (không lưu vào database). */
const val QUICK_MAX_MINUTES = 15

@StringRes
private fun SortOption.labelRes(): Int = when (this) {
    SortOption.NONE -> R.string.sort_default
    SortOption.PRICE_ASC -> R.string.sort_price_asc
    SortOption.PRICE_DESC -> R.string.sort_price_desc
    SortOption.TIME_ASC -> R.string.sort_time_asc
}

fun formatPrice(price: Double): String = String.format(Locale.getDefault(), "%,.0f", price)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishListScreen(
    onDishClick: (Long) -> Unit,
    onAddClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    viewModel: DishListViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val filter by viewModel.filter.collectAsStateWithLifecycle()
    var showFilters by rememberSaveable { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                // app_name = BCS230117_FoodManager, hiện MSSV trên màn hình cho video
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    // Chọn ngôn ngữ English / Tiếng Việt
                    Box {
                        IconButton(onClick = { showLanguageMenu = true }) {
                            Icon(Icons.Default.Language, contentDescription = stringResource(R.string.language))
                        }
                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false }
                        ) {
                            AppLanguage.entries.forEach { language ->
                                DropdownMenuItem(
                                    text = { Text(language.displayName) },
                                    leadingIcon = {
                                        if (language == LanguageManager.current) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        LanguageManager.set(language)
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = onCategoriesClick) {
                        Icon(Icons.Default.Category, contentDescription = stringResource(R.string.categories))
                    }
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filters))
                    }
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = stringResource(R.string.sort))
                        }
                        DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                            SortOption.entries.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(stringResource(option.labelRes())) },
                                    leadingIcon = {
                                        if (option == filter.sort) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    },
                                    onClick = {
                                        viewModel.onSortChange(option)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_dish))
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = filter.query,
                onValueChange = viewModel::onQueryChange,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text(stringResource(R.string.search_hint)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true
            )

            if (showFilters) {
                FilterPanel(filter = filter, categories = state.categories, viewModel = viewModel)
            }

            val categoryNames = remember(state.categories) {
                state.categories.associate { it.id to it.name }
            }

            when {
                state.isLoading -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }

                state.dishes.isEmpty() -> Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(
                            if (state.hasAnyDish) R.string.empty_no_match else R.string.empty_no_dishes
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(24.dp)
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.dishes, key = { it.dish.id }) { item ->
                        DishCard(
                            item = item,
                            categoryName = categoryNames[item.dish.dishCategoryId].orEmpty(),
                            onClick = { onDishClick(item.dish.id) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterPanel(
    filter: DishFilter,
    categories: List<DishCategory>,
    viewModel: DishListViewModel
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filter.categoryId == null,
                    onClick = { viewModel.onCategorySelected(null) },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
            }
            items(categories, key = { it.id }) { category ->
                FilterChip(
                    selected = filter.categoryId == category.id,
                    onClick = { viewModel.onCategorySelected(category.id) },
                    label = { Text(category.name) }
                )
            }
        }

        FilterChip(
            selected = filter.onlyAvailable,
            onClick = { viewModel.onOnlyAvailableChange(!filter.onlyAvailable) },
            label = { Text(stringResource(R.string.filter_available_only)) }
        )

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = filter.minPrice,
                onValueChange = viewModel::onMinPriceChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.filter_min_price)) },
                singleLine = true,
                isError = filter.priceRangeInvalid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = filter.maxPrice,
                onValueChange = viewModel::onMaxPriceChange,
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.filter_max_price)) },
                singleLine = true,
                isError = filter.priceRangeInvalid,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }

        if (filter.priceRangeInvalid) {
            Text(
                text = stringResource(R.string.filter_price_range_error),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        TextButton(onClick = viewModel::onClearFilters) {
            Text(stringResource(R.string.filter_clear))
        }
    }
}

@Composable
private fun DishCard(item: DishWithImages, categoryName: String, onClick: () -> Unit) {
    val dish = item.dish
    val thumbUrl = (item.images.firstOrNull { it.isThumbnail } ?: item.images.firstOrNull())?.imageUrl
    val minutes = stringResource(R.string.minutes_format, dish.preparationTime)
    val speed = stringResource(
        if (dish.preparationTime <= QUICK_MAX_MINUTES) R.string.label_quick else R.string.label_regular
    )

    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            if (thumbUrl != null) {
                FoodImage(
                    url = thumbUrl,
                    contentDescription = dish.name,
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp))
                )
            } else {
                // Món chưa có ảnh: hiện biểu tượng món ăn làm placeholder
                Box(
                    modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Restaurant, contentDescription = null)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = dish.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(text = categoryName, style = MaterialTheme.typography.bodySmall)
                Text(text = "${formatPrice(dish.price)} ₫", style = MaterialTheme.typography.bodyLarge)
                Text(text = "$minutes · $speed", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = stringResource(
                        if (dish.isAvailable) R.string.status_available else R.string.status_unavailable
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (dish.isAvailable) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}