package com.gpstrigger.ui

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.gpstrigger.R
import java.security.MessageDigest

private const val AUTHOR = "Stefano Sisti"
private const val LICENSE = "MIT"
private const val REPOSITORY_URL = "https://github.com/sisti86/android-gps-trigger"

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val version = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName }
    val fingerprint = remember { signingCertificateSha256(context) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { AppIcon() },
        title = { Text(stringResource(R.string.app_name)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.about_version, version.orEmpty()))
                Text(stringResource(R.string.about_author, AUTHOR))
                Text(stringResource(R.string.about_license, LICENSE))
                Text(
                    text = REPOSITORY_URL.removePrefix("https://"),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(REPOSITORY_URL)))
                    },
                )
                Text(stringResource(R.string.about_privacy), style = MaterialTheme.typography.bodySmall)
                if (fingerprint != null) {
                    Column {
                        Text(stringResource(R.string.about_signature), style = MaterialTheme.typography.labelMedium)
                        SelectionContainer {
                            Text(
                                fingerprint,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
    )
}

/** The adaptive icon's two layers drawn by hand: painterResource can't load adaptive icons. */
@Composable
private fun AppIcon() {
    Box(Modifier.size(64.dp).clip(CircleShape)) {
        Image(painterResource(R.drawable.ic_launcher_background), null, Modifier.fillMaxSize())
        Image(painterResource(R.drawable.ic_launcher_foreground), null, Modifier.fillMaxSize())
    }
}

/** SHA-256 of the certificate the installed APK is signed with, as shown by `apksigner`. */
@Suppress("DEPRECATION")
private fun signingCertificateSha256(context: Context): String? {
    val packageManager = context.packageManager
    val certificate = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        // apkContentsSigners is the current signer only, without the keys it was rotated from.
        packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            .signingInfo?.apkContentsSigners?.firstOrNull()
    } else {
        packageManager.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            .signatures?.firstOrNull()
    } ?: return null
    return MessageDigest.getInstance("SHA-256").digest(certificate.toByteArray())
        .joinToString(":") { "%02X".format(it) }
}
