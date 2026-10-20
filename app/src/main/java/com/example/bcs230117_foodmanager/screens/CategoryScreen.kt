package com.example.bcs230117_foodmanager.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bcs230117_foodmanager.R
import com.example.bcs230117_foodmanager.data.DishCategory

/** Mục tiêu của hộp thoại thêm/sửa: category = null nghĩa là thêm mới. */
private data class CategoryFormTarget(val category: DishCategory?)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    viewModel: CategoryViewModel = viewModel()
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var formTarget by remember { mutableStateOf<CategoryFormTarget?>(null) }
    var deleteTarget by remember { mutableStateOf<DishCategory?>(null) }

    val errorText = error?.let { stringResource(it.messageRes()) }
    LaunchedEffect(errorText) {
        if (errorText != null) {
            snackbar.showSnackbar(errorText)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { formTarget = CategoryFormTarget(null) }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_category))
            }
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        if (items.isEmpty()) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.no_categories),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(items, key = { it.category.id }) { item ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(item.category.name, style = MaterialTheme.typography.titleMedium)
                                if (item.category.description.isNotBlank()) {
                                    Text(item.category.description, style = MaterialTheme.typography.bodySmall)
                                }
                                Text(
                                    text = stringResource(R.string.category_dish_count, item.dishCount),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            IconButton(onClick = { formTarget = CategoryFormTarget(item.category) }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                            }
                            IconButton(onClick = { deleteTarget = item.category }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                            }
                        }
                    }
                }
            }
        }
    }

    formTarget?.let { target ->
        CategoryFormDialog(
            category = target.category,
            onDismiss = { formTarget = null },
            onSave = { name, description, onError ->
                viewModel.save(
                    id = target.category?.id ?: 0L,
                    name = name,
                    description = description,
                    onSuccess = { formTarget = null },
                    onError = onError
                )
            }
        )
    }

    deleteTarget?.let { category ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.delete_category_title)) },
            text = { Text(stringResource(R.string.delete_category_message, category.name)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(category.id)
                    deleteTarget = null
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}

@Composable
private fun CategoryFormDialog(
    category: DishCategory?,
    onDismiss: () -> Unit,
    onSave: (String, String, (com.example.bcs230117_foodmanager.data.ValidationError) -> Unit) -> Unit
) {
    var name by remember { mutableStateOf(category?.name.orEmpty()) }
    var description by remember { mutableStateOf(category?.description.orEmpty()) }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    val err = errorRes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(stringResource(if (category == null) R.string.add_category else R.string.edit_category))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        errorRes = null
                    },
                    label = { Text(stringResource(R.string.category_name)) },
                    singleLine = true,
                    isError = err != null,
                    supportingText = if (err != null) {
                        { Text(stringResource(err)) }
                    } else null
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.category_description)) },
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(name, description) { error -> errorRes = error.messageRes() }
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}