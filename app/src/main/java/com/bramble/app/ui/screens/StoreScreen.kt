package com.bramble.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bramble.app.R


@Composable
fun StoreScreen() {
    var showCheckout by rememberSaveable { mutableStateOf(false) }

    if (showCheckout) {
        // Placeholder until real payments exist: nothing is charged.
        AlertDialog(
            onDismissRequest = { showCheckout = false },
            title = { Text(stringResource(R.string.store_checkout_title)) },
            text = { Text(stringResource(R.string.store_checkout_message)) },
            confirmButton = {
                TextButton(onClick = { showCheckout = false }) { Text(stringResource(R.string.store_ok)) }
            },
        )
    }

    Column(
        Modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        StoreItemCard(
            image = R.drawable.raccoon_hat,
            name = stringResource(R.string.store_item_hat),
            price = stringResource(R.string.store_item_hat_price),
            onBuy = { showCheckout = true },
        )
    }
}

/** A product in a double picture-frame border with a diamond ornament on each corner. */
@Composable
private fun StoreItemCard(image: Int, name: String, price: String, onBuy: () -> Unit) {
    val frameColor = MaterialTheme.colorScheme.secondary
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(4.dp, frameColor),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(8.dp)
                .border(1.5.dp, frameColor, RoundedCornerShape(14.dp))
                .drawBehind {
                    val r = 7.dp.toPx()
                    listOf(
                        Offset(0f, 0f),
                        Offset(size.width, 0f),
                        Offset(0f, size.height),
                        Offset(size.width, size.height),
                    ).forEach { c ->
                        val diamond = Path().apply {
                            moveTo(c.x, c.y - r)
                            lineTo(c.x + r, c.y)
                            lineTo(c.x, c.y + r)
                            lineTo(c.x - r, c.y)
                            close()
                        }
                        drawPath(diamond, frameColor)
                    }
                }
                .padding(20.dp),
        ) {
            Image(
                painter = painterResource(image),
                contentDescription = name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(280.dp),
            )
            Text(
                name,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = frameColor,
                modifier = Modifier.padding(top = 12.dp),
            )
            Text(price, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 4.dp))
            Button(onClick = onBuy, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Text(stringResource(R.string.store_buy))
            }
        }
    }
}
