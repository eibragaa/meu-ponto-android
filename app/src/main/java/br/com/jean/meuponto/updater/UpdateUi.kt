package br.com.jean.meuponto.updater

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.OpenInBrowser
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

const val PRIVACY_POLICY_URL =
    "https://github.com/${AppUpdater.GITHUB_OWNER}/${AppUpdater.GITHUB_REPO}/blob/main/docs/privacy-policy.md"

private fun formatFileSize(bytes: Long): String {
    if (bytes <= 0) return "0 MB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.1f MB", mb)
}

/**
 * Diálogo do atualizador in-app, do "tem versão nova" ao "instalar".
 *
 * O fluxo reflete [UpdateDownloadState]: baixar mostra progresso, concluir pede permissão de
 * instalação (`REQUEST_INSTALL_PACKAGES`) e só então abre o instalador do sistema. Falha de
 * assinatura aparece aqui como erro porque o Android só a detecta no momento da instalação.
 */
@Composable
fun UpdateDialog(
    updateInfo: AppUpdateInfo,
    currentVersion: String,
    onDismiss: () -> Unit,
    onInstallerReady: (AppUpdateInfo) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val releaseUrl = updateInfo.releaseUrl.ifBlank {
        "https://github.com/${AppUpdater.GITHUB_OWNER}/${AppUpdater.GITHUB_REPO}"
    }
    var downloadState by remember { mutableStateOf<UpdateDownloadState>(UpdateDownloadState.Idle) }
    var downloadJob by remember { mutableStateOf<Job?>(null) }

    val managePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val state = downloadState
        if (state is UpdateDownloadState.ReadyToInstall && AppUpdater.canInstallUnknownApps(context)) {
            AppUpdater.launchInstaller(context, state.apkFile)
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (downloadState !is UpdateDownloadState.Downloading) {
                onDismiss()
            }
        },
        icon = {
            Icon(
                imageVector = Icons.Outlined.SystemUpdate,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
        },
        title = {
            Text(
                text = "Nova versão disponível",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Atual: v$currentVersion",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "➔",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Nova: v${updateInfo.versionName}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (updateInfo.releaseNotes.isNotBlank()) {
                    Text(
                        text = "Novidades desta versão:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = updateInfo.releaseNotes,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                TextButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(releaseUrl)))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.OpenInBrowser,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Ver no GitHub")
                }

                when (val state = downloadState) {
                    is UpdateDownloadState.Idle -> {
                        if (updateInfo.apkSize > 0) {
                            Text(
                                text = "Tamanho: ${formatFileSize(updateInfo.apkSize)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    is UpdateDownloadState.Downloading -> {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            if (state.progress >= 0f) {
                                LinearProgressIndicator(
                                    progress = { state.progress },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${(state.progress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "${formatFileSize(state.downloadedBytes)} / ${formatFileSize(state.totalBytes)}",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            } else {
                                LinearProgressIndicator(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                )
                                Text(
                                    text = "Baixando (${formatFileSize(state.downloadedBytes)})...",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    is UpdateDownloadState.ReadyToInstall -> {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.TaskAlt,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (AppUpdater.canInstallUnknownApps(context)) {
                                        "Download concluído! Toque em 'Instalar atualização' para continuar."
                                    } else {
                                        "Download pronto. Toque em 'Autorizar e instalar' para permitir a instalação nas configurações."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                    is UpdateDownloadState.Error -> {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (val state = downloadState) {
                is UpdateDownloadState.Idle -> {
                    Button(
                        onClick = {
                            downloadJob = scope.launch {
                                downloadState = UpdateDownloadState.Downloading(0f, 0L, updateInfo.apkSize)
                                try {
                                    val file = AppUpdater.downloadApk(
                                        context = context,
                                        apkUrl = updateInfo.apkUrl,
                                        fileName = updateInfo.apkFileName,
                                        onProgress = { progress, downloaded, total ->
                                            downloadState = UpdateDownloadState.Downloading(progress, downloaded, total)
                                        }
                                    )
                                    downloadState = UpdateDownloadState.ReadyToInstall(file)
                                    onInstallerReady(updateInfo)
                                    if (AppUpdater.canInstallUnknownApps(context)) {
                                        AppUpdater.launchInstaller(context, file)
                                    }
                                } catch (e: Exception) {
                                    downloadState = UpdateDownloadState.Error(
                                        e.localizedMessage ?: "Falha ao baixar a atualização."
                                    )
                                }
                            }
                        }
                    ) {
                        Text("Atualizar agora")
                    }
                }
                is UpdateDownloadState.Downloading -> {
                    OutlinedButton(
                        onClick = {
                            downloadJob?.cancel()
                            downloadState = UpdateDownloadState.Idle
                        }
                    ) {
                        Text("Cancelar")
                    }
                }
                is UpdateDownloadState.ReadyToInstall -> {
                    Button(
                        onClick = {
                            if (AppUpdater.canInstallUnknownApps(context)) {
                                AppUpdater.launchInstaller(context, state.apkFile)
                            } else {
                                managePermissionLauncher.launch(
                                    AppUpdater.createManageUnknownAppSourcesIntent(context)
                                )
                            }
                        }
                    ) {
                        Text(
                            if (AppUpdater.canInstallUnknownApps(context)) {
                                "Instalar atualização"
                            } else {
                                "Autorizar e instalar"
                            }
                        )
                    }
                }
                is UpdateDownloadState.Error -> {
                    Button(
                        onClick = {
                            downloadState = UpdateDownloadState.Idle
                        }
                    ) {
                        Text("Tentar novamente")
                    }
                }
            }
        },
        dismissButton = {
            if (downloadState !is UpdateDownloadState.Downloading) {
                TextButton(onClick = onDismiss) {
                    Text(if (downloadState is UpdateDownloadState.ReadyToInstall) "Fechar" else "Mais tarde")
                }
            }
        }
    )
}

/**
 * Folha "Sobre": versão instalada, aviso de que o app não é registro oficial de ponto, acesso à
 * política de privacidade e verificação manual de atualização.
 *
 * `updaterEnabled` é `false` na flavor `play` — a Play Store não permite instalar APK por fora, então
 * a opção de buscar atualização simplesmente não aparece lá.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSheet(
    currentVersion: String,
    isCheckingUpdate: Boolean,
    updaterEnabled: Boolean,
    onCheckUpdate: () -> Unit,
    onDismiss: () -> Unit,
    brandMark: @Composable () -> Unit
) {
    val context = LocalContext.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            brandMark()
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Meu Ponto",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Versão $currentVersion",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Outlined.TaskAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Privacidade garantida",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Text(
                        text = "Seus comprovantes e dados nunca saem do seu aparelho sem sua ação voluntária. Sem anúncios, sem rastreamento.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = "Este app não é registro oficial de ponto nem substitui o controle adotado pelo empregador. O OCR pode errar: revise data e horário antes de usar qualquer informação.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(PRIVACY_POLICY_URL)
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Outlined.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Política de privacidade")
            }

            if (updaterEnabled) {
                Button(
                    onClick = onCheckUpdate,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCheckingUpdate,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Buscando atualizações...")
                    } else {
                        Icon(
                            Icons.Outlined.SystemUpdate,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Buscar atualizações agora")
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://github.com/${AppUpdater.GITHUB_OWNER}/${AppUpdater.GITHUB_REPO}")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    Icons.Outlined.OpenInBrowser,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text("Ver projeto no GitHub")
            }
        }
    }
}
