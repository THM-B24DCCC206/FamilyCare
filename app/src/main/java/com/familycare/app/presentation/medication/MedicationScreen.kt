package com.familycare.app.presentation.medication

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DocumentScanner
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicationScreen(
    onNavigateToOcr: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản Lý Thuốc & Lịch Uống", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = onNavigateToOcr) {
                        Icon(
                            Icons.Default.DocumentScanner,
                            contentDescription = "Quét OCR đơn thuốc"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToOcr,
                icon = { Icon(Icons.Default.DocumentScanner, contentDescription = null) },
                text = { Text("Quét đơn thuốc AI") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Amlodipin 5mg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Lịch uống: 08:00 sáng hàng ngày (Uống sau ăn)")
                    Spacer(modifier = Modifier.height(8.dp))
                    FilledTonalButton(onClick = { /* Mark as taken */ }) {
                        Text("Xác nhận đã uống")
                    }
                }
            }
        }
    }
}
