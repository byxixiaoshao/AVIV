package com.bicy.whitenoise.ui.screens.ReverbConfigDialogPart

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.bicy.whitenoise.storage.config.ConfigStorage
import com.bicy.whitenoise.ui.screens.SettingScreenPart.ThankYouDialog
import com.bicy.whitenoise.ui.screens.SettingScreenPart.UnlockPremiumDialog

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
