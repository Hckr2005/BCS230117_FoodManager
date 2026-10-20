package com.example.bcs230117_foodmanager.data

import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import java.util.concurrent.TimeUnit

// >>> DÁN ĐỊA CHỈ MOCKAPI CỦA BẠN VÀO ĐÂY (kết thúc bằng dấu /) <<<
const val MOCKAPI_BASE_URL = "https://6ad753a095e98f36aef24d28.mockapi.io/api/v1/"

// Tên resource và tên field trên MockAPI cũng có tiền tố BCS230117_ như database local.
// Các field để kiểu nullable vì dữ liệu từ server có thể thiếu field; SyncManager sẽ gán giá trị mặc định.

data class CategoryDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("BCS230117_Name") val name: String? = null,
    @SerializedName("BCS230117_Description") val description: String? = null
)

data class ImageDto(
    @SerializedName("BCS230117_ImageUrl") val imageUrl: String? = null,
    @SerializedName("BCS230117_IsThumbnail") val isThumbnail: Boolean? = null
)

data class DishDto(
    @SerializedName("id") val id: String? = null,
    @SerializedName("BCS230117_Name") val name: String? = null,
    @SerializedName("BCS230117_Price") val price: Double? = null,
    @SerializedName("BCS230117_PreparationTime") val preparationTime: Int? = null,
    @SerializedName("BCS230117_IsAvailable") val isAvailable: Boolean? = null,
    @SerializedName("BCS230117_Description") val description: String? = null,
    /** Id của loại món TRÊN MOCKAPI (không phải id local). */
    @SerializedName("BCS230117_DishCategoryId") val categoryId: String? = null,
    @SerializedName("BCS230117_Images") val images: List<ImageDto>? = null
)

interface MockApi {
    @GET("BCS230117_DishCategory")
    suspend fun getCategories(): List<CategoryDto>

    @POST("BCS230117_DishCategory")
    suspend fun createCategory(@Body body: CategoryDto): CategoryDto

    @PUT("BCS230117_DishCategory/{id}")
    suspend fun updateCategory(@Path("id") id: String, @Body body: CategoryDto): CategoryDto

    @DELETE("BCS230117_DishCategory/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<Unit>

    @GET("BCS230117_Dish")
    suspend fun getDishes(): List<DishDto>

    @POST("BCS230117_Dish")
    suspend fun createDish(@Body body: DishDto): DishDto

    @PUT("BCS230117_Dish/{id}")
    suspend fun updateDish(@Path("id") id: String, @Body body: DishDto): DishDto

    @DELETE("BCS230117_Dish/{id}")
    suspend fun deleteDish(@Path("id") id: String): Response<Unit>
}

object MockApiClient {
    /** false nếu bạn chưa dán địa chỉ MockAPI; khi đó app chỉ chạy local, không đồng bộ. */
    val isConfigured: Boolean get() = !MOCKAPI_BASE_URL.contains("PASTE")

    val api: MockApi by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
        Retrofit.Builder()
            .baseUrl(MOCKAPI_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MockApi::class.java)
    }
}