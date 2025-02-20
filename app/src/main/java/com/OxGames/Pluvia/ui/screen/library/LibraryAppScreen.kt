package com.OxGames.Pluvia.ui.screen.library

import android.content.Intent
import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.window.core.layout.WindowWidthSizeClass
import com.OxGames.Pluvia.Constants
import com.OxGames.Pluvia.R
import com.OxGames.Pluvia.data.SteamApp
import com.OxGames.Pluvia.service.SteamService
import com.OxGames.Pluvia.ui.component.LoadingScreen
import com.OxGames.Pluvia.ui.component.dialog.ContainerConfigDialog
import com.OxGames.Pluvia.ui.component.dialog.LoadingDialog
import com.OxGames.Pluvia.ui.component.dialog.MessageDialog
import com.OxGames.Pluvia.ui.component.dialog.state.MessageDialogState
import com.OxGames.Pluvia.ui.component.topbar.BackButton
import com.OxGames.Pluvia.ui.data.AppMenuOption
import com.OxGames.Pluvia.ui.enums.AppOptionMenuType
import com.OxGames.Pluvia.ui.enums.DialogType
import com.OxGames.Pluvia.ui.internal.fakeAppInfo
import com.OxGames.Pluvia.ui.screen.library.components.GameInfoRow
import com.OxGames.Pluvia.ui.theme.PluviaTheme
import com.OxGames.Pluvia.utils.ContainerUtils
import com.OxGames.Pluvia.utils.StorageUtils
import com.google.android.play.core.splitcompat.SplitCompat
import com.skydoves.landscapist.ImageOptions
import com.skydoves.landscapist.coil.CoilImage
import com.winlator.container.ContainerData
import com.winlator.xenvironment.ImageFsInstaller
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

// https://partner.steamgames.com/doc/store/assets/libraryassets#4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScreen(
    appId: Int,
    onClickPlay: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var downloadInfo by remember(appId) {
        mutableStateOf(SteamService.getAppDownloadInfo(appId))
    }
    var downloadProgress by remember(appId) {
        mutableFloatStateOf(downloadInfo?.getProgress() ?: 0f)
    }
    var isInstalled by remember(appId) {
        mutableStateOf(SteamService.isAppInstalled(appId))
    }

    val isDownloading: () -> Boolean = { downloadInfo != null && downloadProgress < 1f }

    var loadingDialogVisible by rememberSaveable { mutableStateOf(false) }
    var loadingProgress by rememberSaveable { mutableFloatStateOf(0f) }

    val appInfo by remember(appId) {
        mutableStateOf(SteamService.getAppInfoOf(appId)!!)
    }

    var msgDialogState by rememberSaveable(stateSaver = MessageDialogState.Saver) {
        mutableStateOf(MessageDialogState(false))
    }

    var showConfigDialog by rememberSaveable { mutableStateOf(false) }

    var containerData by rememberSaveable(stateSaver = ContainerData.Saver) {
        mutableStateOf(ContainerData())
    }

    val showEditConfigDialog: () -> Unit = {
        val container = ContainerUtils.getOrCreateContainer(context, appId)
        containerData = ContainerUtils.toContainerData(container)
        showConfigDialog = true
    }

    DisposableEffect(downloadInfo) {
        val onDownloadProgress: (Float) -> Unit = {
            if (it >= 1f) {
                isInstalled = SteamService.isAppInstalled(appId)
            }
            downloadProgress = it
        }

        downloadInfo?.addProgressListener(onDownloadProgress)

        onDispose {
            downloadInfo?.removeProgressListener(onDownloadProgress)
        }
    }

    LaunchedEffect(appId) {
        Timber.d("Selected app $appId")
    }

    val windowWidth = currentWindowAdaptiveInfo().windowSizeClass.windowWidthSizeClass

    val onDismissRequest: (() -> Unit)?
    val onDismissClick: (() -> Unit)?
    val onConfirmClick: (() -> Unit)?
    when (msgDialogState.type) {
        DialogType.CANCEL_APP_DOWNLOAD -> {
            onConfirmClick = {
                downloadInfo?.cancel()
                SteamService.deleteApp(appId)
                downloadInfo = null
                downloadProgress = 0f
                isInstalled = SteamService.isAppInstalled(appId)
                msgDialogState = MessageDialogState(false)
            }
            onDismissRequest = { msgDialogState = MessageDialogState(false) }
            onDismissClick = { msgDialogState = MessageDialogState(false) }
        }

        DialogType.NOT_ENOUGH_SPACE -> {
            onDismissRequest = { msgDialogState = MessageDialogState(false) }
            onConfirmClick = { msgDialogState = MessageDialogState(false) }
            onDismissClick = null
        }

        DialogType.INSTALL_APP -> {
            onDismissRequest = { msgDialogState = MessageDialogState(false) }
            onConfirmClick = {
                CoroutineScope(Dispatchers.IO).launch {
                    downloadProgress = 0f
                    downloadInfo = SteamService.downloadApp(appId)
                    msgDialogState = MessageDialogState(false)
                }
            }
            onDismissClick = { msgDialogState = MessageDialogState(false) }
        }

        DialogType.DELETE_APP -> {
            onConfirmClick = {
                SteamService.deleteApp(appId)
                msgDialogState = MessageDialogState(false)

                isInstalled = SteamService.isAppInstalled(appId)
            }
            onDismissRequest = { msgDialogState = MessageDialogState(false) }
            onDismissClick = { msgDialogState = MessageDialogState(false) }
        }

        DialogType.INSTALL_IMAGEFS -> {
            onDismissRequest = { msgDialogState = MessageDialogState(false) }
            onDismissClick = { msgDialogState = MessageDialogState(false) }
            onConfirmClick = {
                loadingDialogVisible = true
                msgDialogState = MessageDialogState(false)
                CoroutineScope(Dispatchers.IO).launch {
                    if (!SteamService.isImageFsInstallable(context)) {
                        SteamService.downloadImageFs(
                            onDownloadProgress = { loadingProgress = it },
                            this,
                        ).await()
                    }
                    if (!SteamService.isImageFsInstalled(context)) {
                        SplitCompat.install(context)
                        ImageFsInstaller.installIfNeededFuture(context, context.assets) {
                            // Log.d("XServerScreen", "$progress")
                            loadingProgress = it / 100f
                        }.get()
                    }
                    loadingDialogVisible = false
                    showEditConfigDialog()
                }
            }
        }

        else -> {
            onDismissRequest = null
            onDismissClick = null
            onConfirmClick = null
        }
    }

    MessageDialog(
        visible = msgDialogState.visible,
        onDismissRequest = onDismissRequest,
        onConfirmClick = onConfirmClick,
        confirmBtnText = msgDialogState.confirmBtnText,
        onDismissClick = onDismissClick,
        dismissBtnText = msgDialogState.dismissBtnText,
        icon = msgDialogState.type.icon,
        title = msgDialogState.title,
        message = msgDialogState.message,
    )

    ContainerConfigDialog(
        visible = showConfigDialog,
        title = "${appInfo.name} Config",
        initialConfig = containerData,
        onDismissRequest = { showConfigDialog = false },
        onSave = {
            showConfigDialog = false
            ContainerUtils.applyToContainer(context, appId, it)
        },
    )

    LoadingDialog(
        visible = loadingDialogVisible,
        progress = loadingProgress,
    )

    Scaffold(
        topBar = {
            // Show Top App Bar when in Compact or Medium screen space.
            if (windowWidth == WindowWidthSizeClass.COMPACT || windowWidth == WindowWidthSizeClass.MEDIUM) {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = appInfo.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    navigationIcon = {
                        BackButton(onClick = onBack)
                    },
                )
            }
        },
    ) { paddingValues ->
        AppScreenContent(
            modifier = Modifier.padding(paddingValues),
            appInfo = appInfo,
            isInstalled = isInstalled,
            isDownloading = isDownloading(),
            downloadProgress = downloadProgress,
            onDownloadBtnClick = {
                if (isDownloading()) {
                    msgDialogState = MessageDialogState(
                        visible = true,
                        type = DialogType.CANCEL_APP_DOWNLOAD,
                        title = context.getString(R.string.cancel_download_prompt_title),
                        message = "Are you sure you want to cancel the download of the app?",
                        confirmBtnText = context.getString(R.string.yes),
                        dismissBtnText = context.getString(R.string.no),
                    )
                } else if (!isInstalled) {
                    val depots = SteamService.getDownloadableDepots(appId)
                    Timber.d("There are ${depots.size} depots belonging to $appId")
                    // TODO: get space available based on where user wants to install
                    val availableBytes =
                        StorageUtils.getAvailableSpace(context.filesDir.absolutePath)
                    val availableSpace = StorageUtils.formatBinarySize(availableBytes)
                    // TODO: un-hardcode "public" branch
                    val downloadSize = StorageUtils.formatBinarySize(
                        depots.values.sumOf {
                            it.manifests["public"]?.download ?: 0
                        },
                    )
                    val installBytes = depots.values.sumOf { it.manifests["public"]?.size ?: 0 }
                    val installSize = StorageUtils.formatBinarySize(installBytes)
                    if (availableBytes < installBytes) {
                        msgDialogState = MessageDialogState(
                            visible = true,
                            type = DialogType.NOT_ENOUGH_SPACE,
                            title = context.getString(R.string.not_enough_space),
                            message = "The app being installed needs $installSize of space but " +
                                "there is only $availableSpace left on this device",
                            confirmBtnText = context.getString(R.string.acknowledge),
                        )
                    } else {
                        msgDialogState = MessageDialogState(
                            visible = true,
                            type = DialogType.INSTALL_APP,
                            title = context.getString(R.string.download_prompt_title),
                            message = "The app being installed has the following space requirements. Would you like to proceed?" +
                                "\n\n\tDownload Size: $downloadSize" +
                                "\n\tSize on Disk: $installSize" +
                                "\n\tAvailable Space: $availableSpace",
                            confirmBtnText = context.getString(R.string.proceed),
                            dismissBtnText = context.getString(R.string.cancel),
                        )
                    }
                } else {
                    onClickPlay(false)
                }
            },
            optionsMenu = arrayOf(
                AppMenuOption(
                    optionType = AppOptionMenuType.StorePage,
                    onClick = {
                        // TODO add option to view web page externally or internally
                        val browserIntent = Intent(
                            Intent.ACTION_VIEW,
                            (Constants.Library.STORE_URL + appId).toUri(),
                        )
                        context.startActivity(browserIntent)
                    },
                ),
                AppMenuOption(
                    optionType = AppOptionMenuType.EditContainer,
                    onClick = {
                        if (!SteamService.isImageFsInstalled(context)) {
                            if (!SteamService.isImageFsInstallable(context)) {
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.INSTALL_IMAGEFS,
                                    title = "Download & Install ImageFS",
                                    message = "The Ubuntu image needs to be downloaded and installed before " +
                                        "being able to edit the configuration. This operation might take " +
                                        "a few minutes. Would you like to continue?",
                                    confirmBtnText = "Proceed",
                                    dismissBtnText = "Cancel",
                                )
                            } else {
                                msgDialogState = MessageDialogState(
                                    visible = true,
                                    type = DialogType.INSTALL_IMAGEFS,
                                    title = "Install ImageFS",
                                    message = "The Ubuntu image needs to be installed before being able to edit " +
                                        "the configuration. This operation might take a few minutes. " +
                                        "Would you like to continue?",
                                    confirmBtnText = "Proceed",
                                    dismissBtnText = "Cancel",
                                )
                            }
                        } else {
                            showEditConfigDialog()
                        }
                    },
                ),
                *(
                    if (isInstalled) {
                        arrayOf(
                            AppMenuOption(
                                AppOptionMenuType.RunContainer,
                                onClick = {
                                    onClickPlay(true)
                                },
                            ),
                            AppMenuOption(
                                AppOptionMenuType.Uninstall,
                                onClick = {
                                    val sizeOnDisk = StorageUtils.formatBinarySize(
                                        StorageUtils.getFolderSize(SteamService.getAppDirPath(appId)),
                                    )
                                    // TODO: show loading screen of delete progress
                                    msgDialogState = MessageDialogState(
                                        visible = true,
                                        type = DialogType.DELETE_APP,
                                        title = context.getString(R.string.delete_prompt_title),
                                        message = "Are you sure you want to delete this app?\n\n\tSize on Disk: $sizeOnDisk",
                                        confirmBtnText = context.getString(R.string.delete_app),
                                        dismissBtnText = context.getString(R.string.cancel),
                                    )
                                },
                            ),
                        )
                    } else {
                        emptyArray()
                    }
                    ),
            ),
        )
    }
}

@Composable
private fun AppScreenContent(
    modifier: Modifier = Modifier,
    appInfo: SteamApp,
    isInstalled: Boolean,
    isDownloading: Boolean,
    downloadProgress: Float,
    onDownloadBtnClick: () -> Unit,
    vararg optionsMenu: AppMenuOption,
) {
    val scrollState = rememberScrollState()
    var optionsMenuVisible by remember { mutableStateOf(false) }

    LaunchedEffect(appInfo.id) {
        scrollState.animateScrollTo(0)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.Start,
    ) {
        // Images
        Box {
            // (Hero Logo) Steam Partner:
            //  Size: 3840px x 1240px
            //  (an additional half-size 1920px x 620px PNG will be auto-generated from larger file)
            CoilImage(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(with(LocalDensity.current) { 620.toDp() }),
                imageModel = { appInfo.getHeroUrl() },
                imageOptions = ImageOptions(contentScale = ContentScale.None),
                loading = { LoadingScreen() },
                failure = {
                    Box(
                        modifier = Modifier.padding(16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            modifier = Modifier.align(Alignment.TopCenter),
                            text = appInfo.name,
                            style = MaterialTheme.typography.displayLarge,
                            textAlign = TextAlign.Center,
                        )
                        Text(
                            modifier = Modifier.align(Alignment.BottomEnd),
                            text = "[Image not found]",
                        )
                    }
                },
                previewPlaceholder = painterResource(R.drawable.testhero),
            )

            // (Library Logo) Steam Partner:
            //  Size: 1280px x 720px
            //  (an additional 640px x 360px PNG will be auto-generated from larger file)
            CoilImage(
                modifier = Modifier
                    .fillMaxWidth(.45f)
                    .padding(start = 16.dp)
                    .height(with(LocalDensity.current) { 360.toDp() })
                    .align(Alignment.BottomStart),
                imageModel = { appInfo.getLogoUrl() },
                imageOptions = ImageOptions(contentScale = ContentScale.Fit),
                loading = { LoadingScreen() },
                previewPlaceholder = painterResource(R.drawable.testliblogo),
            )
        }

        HorizontalDivider(modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(8.dp))

        // Controls Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .wrapContentHeight(),
        ) {
            FilledTonalButton(
                modifier = Modifier.width(96.dp), // Fixed width to stop button jumping.
                onClick = onDownloadBtnClick,
                content = {
                    val text = if (isInstalled) {
                        stringResource(R.string.run_app)
                    } else if (isDownloading) {
                        stringResource(R.string.cancel)
                    } else {
                        stringResource(R.string.install_app)
                    }
                    Text(text = text)
                },
            )

            Crossfade(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                targetState = isDownloading,
            ) { state ->
                if (state) {
                    Column {
                        Text(
                            modifier = Modifier.align(Alignment.End),
                            text = "${(downloadProgress * 100f).toInt()}%",
                        )
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            progress = { downloadProgress },
                        )
                    }
                }
            }

            Box {
                IconButton(
                    onClick = { optionsMenuVisible = !optionsMenuVisible },
                    content = { Icon(Icons.Filled.MoreVert, "Options") },
                )
                DropdownMenu(
                    expanded = optionsMenuVisible,
                    onDismissRequest = { optionsMenuVisible = false },
                ) {
                    optionsMenu.forEach { menuOption ->
                        DropdownMenuItem(
                            text = { Text(menuOption.optionType.text) },
                            onClick = {
                                menuOption.onClick()
                                optionsMenuVisible = false
                            },
                        )
                    }
                }
            }
        }
        // Game info
        Card(modifier = Modifier.padding(16.dp)) {
            Column {
                val date = remember(appInfo.releaseDate) {
                    val date = Date(appInfo.releaseDate.times(1000))
                    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                }

                GameInfoRow(key = "Controller Support:", value = appInfo.controllerSupport.name)
                GameInfoRow(key = "Developer:", value = appInfo.developer)
                GameInfoRow(key = "Publisher:", value = appInfo.publisher)
                GameInfoRow(key = "Release date:", value = date)
            }
        }
    }
}

/***********
 * PREVIEW *
 ***********/

@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL)
@Preview(
    device = "spec:width=1920px,height=1080px,dpi=440",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL,
) // Odin2 Mini
@Composable
private fun Preview_AppScreen() {
    val context = LocalContext.current
    val intent = Intent(context, SteamService::class.java)
    context.startForegroundService(intent)
    var isDownloading by remember { mutableStateOf(false) }
    PluviaTheme {
        Surface {
            AppScreenContent(
                appInfo = fakeAppInfo(1),
                isInstalled = false,
                isDownloading = isDownloading,
                downloadProgress = .50f,
                onDownloadBtnClick = { isDownloading = !isDownloading },
                optionsMenu = AppOptionMenuType.entries.map {
                    AppMenuOption(
                        optionType = it,
                        onClick = { },
                    )
                }.toTypedArray(),
            )
        }
    }
}
