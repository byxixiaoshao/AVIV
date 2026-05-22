package com.bicy.whitenoise.yODW.NvYq.EF5M

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bicy.whitenoise.JwJY.NATg.ConfigStorage
import com.bicy.whitenoise.yODW.NvYq.BxAd.ThankYouDialog
import com.bicy.whitenoise.yODW.NvYq.BxAd.UnlockPremiumDialog

@Composable
fun PremiumRequiredReverbDialog(
    onDismiss: () -> Unit
) {
    var showThankDialog by remember { mutableStateOf(false) }
    
    if (showThankDialog) {
        ThankYouDialog(
            onConfirm = {
                showThankDialog = false
                ConfigStorage.setPremiumUser(true)
            },
            onDismiss = { showThankDialog = false }
        )
    } else {
        UnlockPremiumDialog(
            isPremium = false,
            onDismiss = onDismiss,
            onPayClick = {
                showThankDialog = true
            }
        )
    }
}
