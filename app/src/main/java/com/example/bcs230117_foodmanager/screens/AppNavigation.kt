package com.example.bcs230117_foodmanager.screens

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

object Routes {
    const val LIST = "list"
    const val DETAIL = "detail/{dishId}"
    const val FORM = "form/{dishId}" // dishId = 0: thêm mới, > 0: sửa
    const val CATEGORIES = "categories"

    fun detail(id: Long) = "detail/$id"
    fun form(id: Long) = "form/$id"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val dishIdArg = listOf(navArgument("dishId") { type = NavType.LongType })

    NavHost(navController = navController, startDestination = Routes.LIST) {
        composable(Routes.LIST) {
            DishListScreen(
                onDishClick = { id -> navController.navigate(Routes.detail(id)) },
                onAddClick = { navController.navigate(Routes.form(0L)) },
                onCategoriesClick = { navController.navigate(Routes.CATEGORIES) }
            )
        }
        composable(Routes.DETAIL, arguments = dishIdArg) {
            DishDetailScreen(
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate(Routes.form(id)) }
            )
        }
        composable(Routes.FORM, arguments = dishIdArg) {
            DishFormScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.CATEGORIES) {
            CategoryScreen(onBack = { navController.popBackStack() })
        }
    }
}