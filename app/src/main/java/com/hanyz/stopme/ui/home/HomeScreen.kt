package com.hanyz.stopme.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hanyz.stopme.R
import com.hanyz.stopme.model.TransportServiceType
import com.hanyz.stopme.ui.theme.NavyPrimary
import com.hanyz.stopme.ui.theme.ServiceTileBlue
import com.hanyz.stopme.ui.theme.TextSecondary
import com.hanyz.stopme.ui.theme.TextWhite

// Home mengikuti desain Figma: header navy + lembaran putih bersudut atas membulat
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSearchChange: (String) -> Unit,
    onProfileClick: () -> Unit,
    onServiceSelected: (TransportServiceType) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(NavyPrimary)
    ) {
        // Header navy
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 22.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SearchPill(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchChange,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(12.dp))
                // Ikon profil putih polos
                Icon(
                    painter = painterResource(id = R.drawable.ic_nav_profile),
                    contentDescription = stringResource(id = R.string.tab_profile),
                    tint = TextWhite,
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { onProfileClick() }
                        .testTag("btn_home_profile")
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Banner utuh sesuai rasio aslinya (16:9) agar logo dan tulisan tidak terpotong
            Image(
                painter = painterResource(id = R.drawable.img_banner_home),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(18.dp))
                    .testTag("banner_home")
            )
        }

        // Lembaran putih yang menumpuk di atas header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.White, RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp))
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(id = R.string.choose_transport_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = NavyPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Grid 2 kolom berlebar tetap, rapat ke tengah seperti Figma
            uiState.filteredServices.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(33.dp, Alignment.CenterHorizontally)
                ) {
                    rowItems.forEach { service ->
                        ServiceTile(
                            service = service,
                            onClick = { onServiceSelected(service) },
                            modifier = Modifier.width(110.dp)
                        )
                    }
                    if (rowItems.size == 1) Spacer(modifier = Modifier.width(110.dp))
                }
            }

            if (uiState.filteredServices.isEmpty()) {
                Text(
                    text = "Layanan tidak ditemukan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

// Kolom pencarian putih berbentuk pil
@Composable
private fun SearchPill(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(46.dp)
            .background(Color.White, RoundedCornerShape(23.dp))
            .padding(horizontal = 14.dp)
            .testTag("search_service_input"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_search),
            contentDescription = null,
            tint = Color.Black,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.search_service_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    color = NavyPrimary.copy(alpha = 0.75f)
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = NavyPrimary),
                cursorBrush = SolidColor(NavyPrimary),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// Kotak layanan kecil berlatar biru keabu-abuan, gambar kendaraan sedikit keluar dari kotak
@Composable
private fun ServiceTile(
    service: TransportServiceType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clickable { onClick() }
            .testTag("service_card_${service.name}"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Ukuran & posisi gambar diukur dari desain Figma (kotak 73,5x64 px, diskalakan ke 84x73 dp)
        val spec = tileSpec(service)
        Box(modifier = Modifier.size(width = 84.dp, height = 73.dp)) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(ServiceTileBlue, RoundedCornerShape(20.dp))
            )
            val imageRes = serviceImage(service)
            if (imageRes == null || spec == null) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_train_white),
                    contentDescription = service.title,
                    tint = TextWhite,
                    modifier = Modifier
                        .size(36.dp)
                        .align(Alignment.Center)
                )
            } else {
                Image(
                    painter = painterResource(id = imageRes),
                    contentDescription = service.title,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .requiredWidth(spec.width)
                        .align(Alignment.BottomStart)
                        .offset(x = spec.offsetX, y = spec.offsetY)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = service.title,
            style = MaterialTheme.typography.bodyMedium,
            color = NavyPrimary,
            textAlign = TextAlign.Center
        )
    }
}

// Lebar gambar dan pergeseran dari pojok kiri-bawah kotak, sesuai Figma
private data class TileImageSpec(
    val width: androidx.compose.ui.unit.Dp,
    val offsetX: androidx.compose.ui.unit.Dp,
    val offsetY: androidx.compose.ui.unit.Dp
)

private fun tileSpec(service: TransportServiceType): TileImageSpec? = when (service) {
    TransportServiceType.TRANSJAKARTA -> TileImageSpec(width = 77.dp, offsetX = 13.dp, offsetY = 1.dp)
    TransportServiceType.JAKLINGKO -> TileImageSpec(width = 77.dp, offsetX = 13.dp, offsetY = 4.dp)
    TransportServiceType.KRL -> TileImageSpec(width = 94.dp, offsetX = (-11).dp, offsetY = 4.dp)
    TransportServiceType.MRT -> TileImageSpec(width = 85.dp, offsetX = (-2).dp, offsetY = 1.dp)
    TransportServiceType.LRT -> null
}

private fun serviceImage(service: TransportServiceType): Int? = when (service) {
    TransportServiceType.TRANSJAKARTA -> R.drawable.img_layanan_transjakarta
    TransportServiceType.JAKLINGKO -> R.drawable.img_layanan_jaklingko
    TransportServiceType.KRL -> R.drawable.img_layanan_krl
    TransportServiceType.MRT -> R.drawable.img_layanan_mrt
    // LRT sedang disembunyikan; aset gambar belum ada, pakai ikon kereta sebagai cadangan
    TransportServiceType.LRT -> null
}
