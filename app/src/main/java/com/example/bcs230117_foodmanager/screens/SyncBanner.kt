package com.example.bcs230117_foodmanager.screens

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bcs230117_foodmanager.R
import com.example.bcs230117_foodmanager.data.SyncCoordinator

/** Thanh nhỏ hiện trạng thái mạng và đồng bộ ngay trên danh sách. */
@Composable
fun SyncBanner() {
    val s by SyncCoordinator.state.collectAsStateWithLifecycle()

    val text = when {
        !s.online && s.pending > 0 -> stringResource(R.string.sync_offline_pending, s.pending)
        !s.online -> stringResource(R.string.sync_offline)
        s.syncing -> stringResource(R.string.sync_syncing)
        s.error -> stringResource(R.string.sync_error)
        s.pending > 0 -> stringResource(R.string.sync_waiting, s.pending)
        else -> stringResource(R.string.sync_done)
    }
    val warning = !s.online || s.error

    Surface(
        color = if (warning) MaterialTheme.colorScheme.errorContainer
        else MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )
    }
}