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
    val navController = rememberNavController()   // NavController : 화면간 이동
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
    val activity = (LocalContext.current as Activity)               // 현재 컨텍스트를 Activity로 변환하고 activity 변수에 저장
    NavHost(navController = navController, startDestination = "home") {  // 시작 화면 home으로 설정
        composable("home") {
            HomeScreen(
                onPlantClick = {
                    navController.navigate("plantDetail/${it.plantId}") // 식물 클릭 시 해당 식물의 상세 정보 이동
                },
                onPageChange = onPageChange,
                onAttached = onAttached,
                plantListViewModel = plantListViewModel
            )
        }
        composable(
            "plantDetail/{plantId}",                // 식물 id의 상세 정보 화면
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
                    navController.navigate("gallery/${it.name}") // 갤러리 클릭 시 해당 식물의 갤러리 화면 이동
                }
            )
        }
        composable(
            "gallery/{plantName}",                  // 식물 이름의 갤러리
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