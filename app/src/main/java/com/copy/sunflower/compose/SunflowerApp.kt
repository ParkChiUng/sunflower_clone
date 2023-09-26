package com.copy.sunflower.compose

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.widget.Toolbar
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.app.ShareCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.copy.sunflower.R
import com.copy.sunflower.compose.gallery.GalleryScreen
import com.copy.sunflower.compose.home.HomeScreen
import com.copy.sunflower.compose.home.SunflowerPage
import com.copy.sunflower.compose.plantdetail.PlantDetailsScreen
import com.copy.sunflower.viewmodels.PlantListViewModel

@Composable
fun SunflowerApp(
    onPageChange: (SunflowerPage) -> Unit = {},
    onAttached: (Toolbar) -> Unit = {},
    plantListViewModel: PlantListViewModel = hiltViewModel(),

) {
    val navController = rememberNavController()
    SunFlowerNavHost(
        plantListViewModel = plantListViewModel,
        navController = navController,
        onPageChange = onPageChange,
        onAttached = onAttached
    )
}

@Composable
fun SunFlowerNavHost(
    navController: NavHostController,
    onPageChange: (SunflowerPage) -> Unit = {},
    onAttached: (Toolbar) -> Unit = {},
    plantListViewModel: PlantListViewModel = hiltViewModel(),
) {
    val activity = (LocalContext.current as Activity)
    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onPlantClick = {
                    navController.navigate("plantDetail/${it.plantId}")
                },
                onPageChange = onPageChange,
                onAttached = onAttached,
                plantListViewModel = plantListViewModel
            )
        }
        composable(
            "plantDetail/{plantId}",
            arguments = listOf(navArgument("plantId") {
                type = NavType.StringType
            })
        ) {
            PlantDetailsScreen(
                onBackClick = { navController.navigateUp() },
                onShareClick = {
                    createShareIntent(activity, it)
                },
                onGalleryClick = {
                    navController.navigate("gallery/${it.name}")
                }
            )
        }
        composable(
            "gallery/{plantName}",
            arguments = listOf(navArgument("plantName") {
                type = NavType.StringType
            })
        ) {
            GalleryScreen(
                onPhotoClick = {
                    val uri = Uri.parse(it.user.attributionUrl)
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    activity.startActivity(intent)
                },
                onUpClick = {
                    navController.navigateUp()
                })
        }
    }
}

private fun createShareIntent(activity: Activity, plantName: String) {
    val shareText = activity.getString(R.string.share_text_plant, plantName)
    val shareIntent = ShareCompat.IntentBuilder(activity)
        .setText(shareText)
        .setType("text/plain")
        .createChooserIntent()
        .addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    activity.startActivity(shareIntent)
}