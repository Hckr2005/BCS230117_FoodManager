package com.example.bcs230117_foodmanager.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bcs230117_foodmanager.R
import com.example.bcs230117_foodmanager.data.DishCategory
import com.example.bcs230117_foodmanager.data.ValidationError

/** Đổi mã lỗi nghiệp vụ sang chuỗi hiển thị (dịch theo ngôn ngữ đang chọn). */
@StringRes
fun ValidationError.messageRes(): Int = when (this) {
    ValidationError.NAME_EMPTY -> R.string.error_name_empty
    ValidationError.NAME_DUPLICATE -> R.string.error_name_duplicate
    ValidationError.PRICE_INVALID -> R.string.error_price_invalid
    ValidationError.PREPARATION_TIME_INVALID -> R.string.error_time_invalid
    ValidationError.CATEGORY_INVALID -> R.string.error_category_invalid
    ValidationError.NOT_FOUND -> R.string.error_not_found
    ValidationError.CATEGORY_IN_USE -> R.string.error_category_in_use
    ValidationError.IMAGE_URL_INVALID -> R.string.error_image_url_invalid
    ValidationError.UNKNOWN -> R.string.error_unknown
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishFormScreen(
    onBack: () -> Unit,
    viewModel: DishFormViewModel = viewModel()
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(if (form.isEdit) R.string.form_edit_title else R.string.form_add_title))
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        when {
            !form.loaded -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            form.notFound -> Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.error_not_found), textAlign = TextAlign.Center)
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.back))
                }
            }

            else -> FormContent(
                form = form,
                categories = categories,
                viewModel = viewModel,
                onBack = onBack,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun FormContent(
    form: DishFormState,
    categories: List<DishCategory>,
    viewModel: DishFormViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val errors = form.errors

    val nameError = when {
        ValidationError.NAME_EMPTY in errors -> R.string.error_name_empty
        ValidationError.NAME_DUPLICATE in errors -> R.string.error_name_duplicate
        else -> null
    }
    val priceError = if (ValidationError.PRICE_INVALID in errors) R.string.error_price_invalid else null
    val timeError = if (ValidationError.PREPARATION_TIME_INVALID in errors) R.string.error_time_invalid else null
    val categoryError = if (ValidationError.CATEGORY_INVALID in errors) R.string.error_category_invalid else null
    val generalError = when {
        ValidationError.NOT_FOUND in errors -> R.string.error_not_found
        ValidationError.UNKNOWN in errors -> R.string.error_unknown
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = form.name,
            onValueChange = viewModel::onNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_name)) },
            singleLine = true,
            isError = nameError != null,
            supportingText = if (nameError != null) {
                { Text(stringResource(nameError)) }
            } else null,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words)
        )

        CategoryDropdown(
            categories = categories,
            selectedId = form.categoryId,
            errorRes = categoryError,
            onSelected = viewModel::onCategoryChange
        )

        OutlinedTextField(
            value = form.price,
            onValueChange = viewModel::onPriceChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_price)) },
            singleLine = true,
            isError = priceError != null,
            supportingText = if (priceError != null) {
                { Text(stringResource(priceError)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = form.preparationTime,
            onValueChange = viewModel::onTimeChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_time)) },
            singleLine = true,
            isError = timeError != null,
            supportingText = if (timeError != null) {
                { Text(stringResource(timeError)) }
            } else null,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        OutlinedTextField(
            value = form.description,
            onValueChange = viewModel::onDescriptionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_description)) },
            minLines = 2,
            maxLines = 4
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(stringResource(R.string.field_available))
            Switch(checked = form.isAvailable, onCheckedChange = viewModel::onAvailableChange)
        }

        if (generalError != null) {
            Text(
                text = stringResource(generalError),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.cancel))
            }
            Button(
                onClick = { viewModel.save(onSaved = onBack) },
                enabled = !form.saving,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

/** Ô chọn loại món: dữ liệu lấy từ database, bấm vào để mở danh sách. */
@Composable
private fun CategoryDropdown(
    categories: List<DishCategory>,
    selectedId: Long?,
    @StringRes errorRes: Int?,
    onSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = categories.firstOrNull { it.id == selectedId }?.name.orEmpty()

    Box(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.field_category)) },
            trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
            isError = errorRes != null,
            supportingText = if (errorRes != null) {
                { Text(stringResource(errorRes)) }
            } else null
        )
        // Lớp phủ bắt thao tác bấm, vì ô chỉ đọc không tự mở menu
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            categories.forEach { category ->
                DropdownMenuItem(
                    text = { Text(category.name) },
                    onClick = {
                        onSelected(category.id)
                        expanded = false
                    }
                )
            }
        }
    }
}