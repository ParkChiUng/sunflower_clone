package com.copy.sunflower.compose.plantdetail

import android.graphics.drawable.Drawable
import android.text.method.LinkMovementMethod
import androidx.annotation.VisibleForTesting
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Share
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidViewBinding
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.text.HtmlCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.google.accompanist.themeadapter.material.MdcTheme
import com.copy.sunflower.R
import com.copy.sunflower.compose.Dimens
import com.copy.sunflower.compose.utils.SunflowerImage
import com.copy.sunflower.compose.utils.TextSnackbarContainer
import com.copy.sunflower.compose.visible
import com.copy.sunflower.data.Plant
import com.copy.sunflower.databinding.ItemPlantDescriptionBinding
import com.copy.sunflower.viewmodels.PlantDetailViewModel

data class PlantDetailsCallbacks(
    val onFabClick: () -> Unit,
    val onBackClick: () -> Unit,
    val onShareClick: (String) -> Unit,
    val onGalleryClick: (Plant) -> Unit
)

@Composable
fun PlantDetailsScreen(
    plantDetailsViewModel: PlantDetailViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    onShareClick: (String) -> Unit,
    onGalleryClick: (Plant) -> Unit,
) {
    val plant = plantDetailsViewModel.plant.observeAsState().value
    val isPlanted = plantDetailsViewModel.isPlanted.collectAsState(initial = false).value
    val showSnackbar = plantDetailsViewModel.showSnackbar.observeAsState().value

    if (plant != null && isPlanted != null && showSnackbar != null) {
        Surface {
            TextSnackbarContainer(
                snackbarText = stringResource(R.string.added_plant_to_garden),
                showSnackbar = showSnackbar,
                onDismissSnackbar = { plantDetailsViewModel.dismissSnackbar() }
            ) {
                PlantDetails(
                    plant,
                    isPlanted,
                    plantDetailsViewModel.hasValidUnsplashKey(),
                    PlantDetailsCallbacks(
                        onBackClick = onBackClick,
                        onFabClick = {
                            plantDetailsViewModel.addPlantToGarden()
                        },
                        onShareClick = onShareClick,
                        onGalleryClick = onGalleryClick,
                    )
                )
            }
        }
    }
}

@VisibleForTesting
@Composable
fun PlantDetails(
    plant: Plant,
    isPlanted: Boolean,
    hasValidUnsplashKey: Boolean,
    callbacks: PlantDetailsCallbacks,
    modifier: Modifier = Modifier
) {
    // PlantDetails owns the scrollerPosition to simulate CollapsingToolbarLayout's behavior
    val scrollState = rememberScrollState()
    var plantScroller by remember {
        mutableStateOf(PlantDetailsScroller(scrollState, Float.MIN_VALUE))
    }
    val transitionState =
        remember(plantScroller) { plantScroller.toolbarTransitionState }
    val toolbarState = plantScroller.getToolbarState(LocalDensity.current)

    // Transition that fades in/out the header with the image and the Toolbar
    val transition = updateTransition(transitionState, label = "")
    val toolbarAlpha = transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) }, label = ""
    ) { toolbarTransitionState ->
        if (toolbarTransitionState == ToolbarState.HIDDEN) 0f else 1f
    }
    val contentAlpha = transition.animateFloat(
        transitionSpec = { spring(stiffness = Spring.StiffnessLow) }, label = ""
    ) { toolbarTransitionState ->
        if (toolbarTransitionState == ToolbarState.HIDDEN) 1f else 0f
    }

    val toolbarHeightPx = with(LocalDensity.current) {
        Dimens.PlantDetailAppBarHeight.roundToPx().toFloat()
    }
    val toolbarOffsetHeightPx = remember { mutableStateOf(0f) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                val newOffset = toolbarOffsetHeightPx.value + delta
                toolbarOffsetHeightPx.value =
                    newOffset.coerceIn(-toolbarHeightPx, 0f)
                return Offset.Zero
            }
        }
    }

    Box(
        modifier
            .fillMaxSize()
            .systemBarsPadding()
            .nestedScroll(nestedScrollConnection)
    ) {
        PlantDetailsContent(
            scrollState = scrollState,
            toolbarState = toolbarState,
            onNamePosition = { newNamePosition ->
                if (plantScroller.namePosition == Float.MIN_VALUE) {
                    plantScroller =
                        plantScroller.copy(namePosition = newNamePosition)
                }
            },
            plant = plant,
            isPlanted = isPlanted,
            hasValidUnsplashKey = hasValidUnsplashKey,
            imageHeight = with(LocalDensity.current) {
                val candidateHeight =
                    Dimens.PlantDetailAppBarHeight + toolbarOffsetHeightPx.value.toDp()
                maxOf(candidateHeight, 1.dp)
            },
            onFabClick = callbacks.onFabClick,
            onGalleryClick = { callbacks.onGalleryClick(plant) },
            contentAlpha = { contentAlpha.value }
        )
        PlantToolbar(
            toolbarState, plant.name, callbacks,
            toolbarAlpha = { toolbarAlpha.value },
            contentAlpha = { contentAlpha.value }
        )
    }
}

@Composable
private fun PlantDetailsContent(
    scrollState: ScrollState,
    toolbarState: ToolbarState,
    plant: Plant,
    isPlanted: Boolean,
    hasValidUnsplashKey: Boolean,
    imageHeight: Dp,
    onNamePosition: (Float) -> Unit,
    onFabClick: () -> Unit,
    onGalleryClick: () -> Unit,
    contentAlpha: () -> Float,
) {
    Column(Modifier.verticalScroll(scrollState)) {
        ConstraintLayout {
            val (image, fab, info) = createRefs()

            PlantImage(
                imageUrl = plant.imageUrl,
                imageHeight = imageHeight,
                modifier = Modifier
                    .constrainAs(image) { top.linkTo(parent.top) }
                    .alpha(contentAlpha())
            )

            if (!isPlanted) {
                val fabEndMargin = Dimens.PaddingSmall
                PlantFab(
                    onFabClick = onFabClick,
                    modifier = Modifier
                        .constrainAs(fab) {
                            centerAround(image.bottom)
                            absoluteRight.linkTo(
                                parent.absoluteRight,
                                margin = fabEndMargin
                            )
                        }
                        .alpha(contentAlpha())
                )
            }

            PlantInformation(
                name = plant.name,
                wateringInterval = plant.wateringInterval,
                description = plant.description,
                hasValidUnsplashKey = hasValidUnsplashKey,
                onNamePosition = { onNamePosition(it) },
                toolbarState = toolbarState,
                onGalleryClick = onGalleryClick,
                modifier = Modifier.constrainAs(info) {
                    top.linkTo(image.bottom)
                }
            )
        }
    }
}

@Composable
private fun PlantImage(
    imageUrl: String,
    imageHeight: Dp,
    modifier: Modifier = Modifier,
    placeholderColor: Color = MaterialTheme.colors.onSurface.copy(0.2f)
) {
    var isLoading by remember { mutableStateOf(true) }
    Box(
        modifier
            .fillMaxWidth()
            .height(imageHeight)
    ) {
        if (isLoading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(placeholderColor)
            )
        }
        SunflowerImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize(),
            contentScale = ContentScale.Crop,
        ) {
            it.addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>?,
                    isFirstResource: Boolean
                ): Boolean {
                    isLoading = false
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    isLoading = false
                    return false
                }
            })
        }
    }
}

@Composable
private fun PlantFab(
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val addPlantContentDescription = stringResource(R.string.add_plant)
    FloatingActionButton(
        onClick = onFabClick,
        shape = MaterialTheme.shapes.small,
        // Semantics in parent due to https://issuetracker.google.com/184825850
        modifier = modifier.semantics {
            contentDescription = addPlantContentDescription
        }
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = null
        )
    }
}

@Composable
private fun PlantToolbar(
    toolbarState: ToolbarState,
    plantName: String,
    callbacks: PlantDetailsCallbacks,
    toolbarAlpha: () -> Float,
    contentAlpha: () -> Float
) {
    val onShareClick = {
        callbacks.onShareClick(plantName)
    }
    if (toolbarState.isShown) {
        PlantDetailsToolbar(
            plantName = plantName,
            onBackClick = callbacks.onBackClick,
            onShareClick = onShareClick,
            modifier = Modifier.alpha(toolbarAlpha())
        )
    } else {
        PlantHeaderActions(
            onBackClick = callbacks.onBackClick,
            onShareClick = onShareClick,
            modifier = Modifier.alpha(contentAlpha())
        )
    }
}

@Composable
private fun PlantDetailsToolbar(
    plantName: String,
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface {
        TopAppBar(
            modifier = modifier.statusBarsPadding(),
            backgroundColor = MaterialTheme.colors.surface
        ) {
            IconButton(
                onBackClick,
                Modifier.align(Alignment.CenterVertically)
            ) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = stringResource(id = R.string.a11y_back)
                )
            }
            Text(
                text = plantName,
                style = MaterialTheme.typography.h6,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            )
            val shareContentDescription =
                stringResource(R.string.menu_item_share_plant)
            IconButton(
                onShareClick,
                Modifier
                    .align(Alignment.CenterVertically)
                    .semantics { contentDescription = shareContentDescription }
            ) {
                Icon(
                    Icons.Filled.Share,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun PlantHeaderActions(
    onBackClick: () -> Unit,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(top = Dimens.ToolbarIconPadding),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        val iconModifier = Modifier
            .sizeIn(
                maxWidth = Dimens.ToolbarIconSize,
                maxHeight = Dimens.ToolbarIconSize
            )
            .background(
                color = MaterialTheme.colors.surface,
                shape = CircleShape
            )

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(start = Dimens.ToolbarIconPadding)
                .then(iconModifier)
        ) {
            Icon(
                Icons.Filled.ArrowBack,
                contentDescription = stringResource(id = R.string.a11y_back)
            )
        }
        val shareContentDescription =
            stringResource(R.string.menu_item_share_plant)
        IconButton(
            onClick = onShareClick,
            modifier = Modifier
                .padding(end = Dimens.ToolbarIconPadding)
                .then(iconModifier)
                .semantics {
                    contentDescription = shareContentDescription
                }
        ) {
            Icon(
                Icons.Filled.Share,
                contentDescription = null
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun PlantInformation(
    name: String,
    wateringInterval: Int,
    description: String,
    hasValidUnsplashKey: Boolean,
    onNamePosition: (Float) -> Unit,
    toolbarState: ToolbarState,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(Dimens.PaddingLarge)) {
        Text(
            text = name,
            style = MaterialTheme.typography.h5,
            modifier = Modifier
                .padding(
                    start = Dimens.PaddingSmall,
                    end = Dimens.PaddingSmall,
                    bottom = Dimens.PaddingNormal
                )
                .align(Alignment.CenterHorizontally)
                .onGloballyPositioned { onNamePosition(it.positionInWindow().y) }
                .visible { toolbarState == ToolbarState.HIDDEN }
        )
        Box(
            Modifier
                .align(Alignment.CenterHorizontally)
                .padding(
                    start = Dimens.PaddingSmall,
                    end = Dimens.PaddingSmall,
                    bottom = Dimens.PaddingNormal
                )
        ) {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(id = R.string.watering_needs_prefix),
                    color = MaterialTheme.colors.primaryVariant,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(horizontal = Dimens.PaddingSmall)
                        .align(Alignment.CenterHorizontally)
                )
                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        text = pluralStringResource(
                            R.plurals.watering_needs_suffix,
                            wateringInterval,
                            wateringInterval
                        ),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                    )
                }
            }
            if (hasValidUnsplashKey) {
                Image(
                    painter = painterResource(id = R.drawable.ic_photo_library),
                    contentDescription = "Gallery Icon",
                    Modifier
                        .clickable { onGalleryClick() }
                        .align(Alignment.CenterEnd)
                )
            }
        }
        PlantDescription(description)
    }
}

@Composable
private fun PlantDescription(description: String) {
    AndroidViewBinding(ItemPlantDescriptionBinding::inflate) {
        plantDescription.text = HtmlCompat.fromHtml(
            description,
            HtmlCompat.FROM_HTML_MODE_COMPACT
        )
        plantDescription.movementMethod = LinkMovementMethod.getInstance()
        plantDescription.linksClickable = true
    }
}

@Preview
@Composable
private fun PlantDetailContentPreview() {
    MdcTheme {
        Surface {
            PlantDetails(
                Plant("plantId", "Tomato", "HTML<br>description", 6),
                true,
                true,
                PlantDetailsCallbacks({ }, { }, { }, { })
            )
        }
    }
}