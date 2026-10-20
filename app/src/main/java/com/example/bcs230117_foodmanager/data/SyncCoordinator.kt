package com.example.bcs230117_foodmanager.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SyncUiState(
    val online: Boolean = true,
    val pending: Int = 0,
    val syncing: Boolean = false,
    val error: Boolean = false
)

/**
 * Theo dõi kết nối mạng và số thay đổi chưa đồng bộ.
 * Có mạng + còn thay đổi chờ (hoặc vừa có mạng trở lại) -> tự đồng bộ, không cần bấm nút.
 */
object SyncCoordinator {
    private var started = false
    private val _state = MutableStateFlow(SyncUiState())
    val state: StateFlow<SyncUiState> = _state.asStateFlow()

    fun start(context: Context) {
        if (started) return
        started = true

        val app = context.applicationContext
        val db = AppDatabase.getInstance(app)
        val online = MutableStateFlow(checkOnline(app))
        registerNetworkCallback(app, online)

        val pending = combine(
            db.categoryDao().observePendingCount(),
            db.dishDao().observePendingCount()
        ) { a, b -> a + b }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        // Cập nhật trạng thái hiển thị (mạng, số thay đổi chờ)
        scope.launch {
            combine(online, pending) { on, n -> on to n }.collect { (on, n) ->
                _state.update { it.copy(online = on, pending = n) }
            }
        }

        // Tự đồng bộ
        scope.launch {
            if (!MockApiClient.isConfigured) return@launch
            val manager = SyncManager(db, MockApiClient.api)
            var wasOnline = false
            combine(online, pending) { on, n -> on to n }.conflate().collect { (on, n) ->
                val cameOnline = on && !wasOnline
                wasOnline = on
                if (!on || !(cameOnline || n > 0)) return@collect

                delay(300)
                var ok = false
                while (!ok && online.value) {
                    _state.update { it.copy(syncing = true) }
                    ok = manager.sync()
                    _state.update { it.copy(syncing = false, error = !ok) }
                    if (!ok) delay(10_000) // lỗi MockAPI: thử lại sau 10 giây
                }
            }
        }
    }

    /** Chỉ cần mạng hiện tại có khả năng Internet (không đòi VALIDATED vì emulator hay không đạt). */
    private fun hasInternet(caps: NetworkCapabilities?): Boolean =
        caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)

    private fun checkOnline(context: Context): Boolean = try {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        hasInternet(cm.getNetworkCapabilities(cm.activeNetwork))
    } catch (e: Exception) {
        false
    }

    private fun registerNetworkCallback(context: Context, online: MutableStateFlow<Boolean>) {
        try {
            val cm = context.getSystemService(ConnectivityManager::class.java)
            cm.registerDefaultNetworkCallback(object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    online.value = hasInternet(cm.getNetworkCapabilities(network))
                }

                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    online.value = hasInternet(caps)
                }

                override fun onLost(network: Network) {
                    online.value = false
                }
            })
        } catch (e: Exception) {
            // Không đăng ký được: vẫn chạy local
        }
    }
}