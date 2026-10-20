package com.example.bcs230117_foodmanager.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bcs230117_foodmanager.R
import com.example.bcs230117_foodmanager.data.DishWithImages
import com.example.bcs230117_foodmanager.data.ValidationError

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishDetailScreen(
    onBack: () -> Unit,
    onEdit: (Long) -> Unit,
    viewModel: DishDetailViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actionError by viewModel.actionError.collectAsStateWithLifecycle()
    var showConfirm by rememberSaveable { mutableStateOf(false) }
    var showAddImage by rememberSaveable { mutableStateOf(false) }
    val snackbar = remember { SnackbarHostState() }

    val errorText = actionError?.let { stringResource(it.messageRes()) }
    LaunchedEffect(errorText) {
        if (errorText != null) {
            snackbar.showSnackbar(errorText)
            viewModel.clearError()
        }
    }

    val current = state.dish

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (current != null) {
                        IconButton(onClick = { onEdit(current.dish.id) }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                        }
                        IconButton(onClick = { showConfirm = true }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbar) }
    ) { padding ->
        when {
            state.isLoading -> Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }

            current == null -> Column(
                modifier = Modifier.padding(padding).fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Id không tồn tại (hoặc món vừa bị xóa)
                Text(
                    text = stringResource(R.string.error_not_found),
                    textAlign = TextAlign.Center
                )
                Button(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
                    Text(stringResource(R.string.back))
                }
            }

            else -> DishDetailContent(
                item = current,
                categoryName = state.categoryName,
                onAddImage = { showAddImage = true },
                onSetThumbnail = viewModel::setThumbnail,
                onDeleteImage = viewModel::deleteImage,
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showConfirm && current != null) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_message, current.dish.name)) },
            confirmButton = {
                TextButton(onClick = {
                    showConfirm = false
                    viewModel.delete(onDone = onBack)
                }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showAddImage) {
        AddImageDialog(
            onDismiss = { showAddImage = false },
            onAdd = { url, onError ->
                viewModel.addImage(url, onSuccess = { showAddImage = false }, onError = onError)
            }
        )
    }
}

@Composable
private fun AddImageDialog(
    onDismiss: () -> Unit,
    onAdd: (String, (ValidationError) -> Unit) -> Unit
) {
    var url by rememberSaveable { mutableStateOf("") }
    var errorRes by remember { mutableStateOf<Int?>(null) }
    val err = errorRes

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_image)) },
        text = {
            OutlinedTextField(
                value = url,
                onValueChange = {
                    url = it
                    errorRes = null
                },
                label = { Text(stringResource(R.string.image_url)) },
                singleLine = true,
                isError = err != null,
                supportingText = if (err != null) {
                    { Text(stringResource(err)) }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri)
            )
        },
        confirmButton = {
            TextButton(onClick = { onAdd(url) { error -> errorRes = error.messageRes() } }) {
                Text(stringResource(R.string.add))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

@Composable
private fun DishDetailContent(
    item: DishWithImages,
    categoryName: String,
    onAddImage: () -> Unit,
    onSetThumbnail: (Long) -> Unit,
    onDeleteImage: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val dish = item.dish
    val minutes = stringResource(R.string.minutes_format, dish.preparationTime)
    val speed = stringResource(
        if (dish.preparationTime <= QUICK_MAX_MINUTES) R.string.label_quick else R.string.label_regular
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(R.string.images), style = MaterialTheme.typography.titleSmall)

        if (item.images.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Restaurant, contentDescription = null)
                    Text(stringResource(R.string.no_images), style = MaterialTheme.typography.bodySmall)
                }
            }
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(item.images, key = { it.id }) { image ->
                    Box(Modifier.width(280.dp).height(200.dp).clip(RoundedCornerShape(12.dp))) {
                        FoodImage(
                            url = image.imageUrl,
                            contentDescription = dish.name,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (image.isThumbnail) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(bottomEnd = 8.dp),
                                modifier = Modifier.align(Alignment.TopStart)
                            ) {
                                Text(
                                    text = stringResource(R.string.thumbnail),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                        // Nút đổi ảnh đại diện và xóa ảnh
                        Surface(
                            color = Color.Black.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(topStart = 12.dp),
                            modifier = Modifier.align(Alignment.BottomEnd)
                        ) {
                            Row {
                                if (!image.isThumbnail) {
                                    IconButton(onClick = { onSetThumbnail(image.id) }) {
                                        Icon(
                                            Icons.Default.StarBorder,
                                            contentDescription = stringResource(R.string.set_thumbnail),
                                            tint = Color.White
                                        )
                                    }
                                }
                                IconButton(onClick = { onDeleteImage(image.id) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.delete_image),
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        OutlinedButton(onClick = onAddImage) {
            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.add_image))
        }

        Text(dish.name, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(top = 8.dp))
        Text(categoryDisplayName(categoryName), style = MaterialTheme.typography.bodyMedium)
        Text("${formatPrice(dish.price)} ₫", style = MaterialTheme.typography.titleLarge)
        Text("$minutes · $speed", style = MaterialTheme.typography.bodyMedium)
        Text(
            text = stringResource(
                if (dish.isAvailable) R.string.status_available else R.string.status_unavailable
            ),
            style = MaterialTheme.typography.labelLarge,
            color = if (dish.isAvailable) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.error
        )
        Text(
            text = dish.description.ifBlank { stringResource(R.string.no_description) },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}