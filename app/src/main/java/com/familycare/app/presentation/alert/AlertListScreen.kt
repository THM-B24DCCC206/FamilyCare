package com.familycare.app.presentation.alert

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * Màn hình danh sách cảnh báo (Chuyển tiếp đến AlertCenterScreen)
 */
@Composable
fun AlertListScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSos: () -> Unit = {},
    viewModel: AlertViewModel = viewModel(
        factory = AlertViewModel.provideFactory(LocalContext.current)
    )
) {
    AlertCenterScreen(
        onNavigateBack = onNavigateBack,
        onNavigateToSos = onNavigateToSos,
        viewModel = viewModel
    )
}
