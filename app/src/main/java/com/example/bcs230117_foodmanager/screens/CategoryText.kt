package com.example.bcs230117_foodmanager.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.bcs230117_foodmanager.R

/**
 * Dịch tên 3 loại món mẫu theo ngôn ngữ đang chọn.
 * Database vẫn lưu tên tiếng Anh; loại do người dùng tự tạo giữ nguyên tên.
 */
@Composable
fun categoryDisplayName(name: String): String = when (name) {
    "Main Course" -> stringResource(R.string.cat_main_course)
    "Drinks" -> stringResource(R.string.cat_drinks)
    "Dessert" -> stringResource(R.string.cat_dessert)
    else -> name
}

/** Dịch mô tả của 3 loại món mẫu (chỉ khi mô tả còn nguyên như dữ liệu mẫu). */
@Composable
fun categoryDisplayDescription(name: String, description: String): String = when {
    name == "Main Course" && description == "Rice, noodle and soup dishes" ->
        stringResource(R.string.cat_main_course_desc)
    name == "Drinks" && description == "Hot and cold beverages" ->
        stringResource(R.string.cat_drinks_desc)
    name == "Dessert" && description == "Sweet dishes" ->
        stringResource(R.string.cat_dessert_desc)
    else -> description
}