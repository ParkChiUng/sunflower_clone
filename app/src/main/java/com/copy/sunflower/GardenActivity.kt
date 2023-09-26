package com.copy.sunflower

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.MenuProvider
import androidx.core.view.WindowCompat
import com.copy.sunflower.compose.SunflowerApp
import com.copy.sunflower.compose.home.SunflowerPage
import com.copy.sunflower.viewmodels.PlantListViewModel
import com.google.accompanist.themeadapter.material.MdcTheme
import android.util.Log
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint

//import androidx.compose.material.MaterialTheme

@AndroidEntryPoint
class GardenActivity : AppCompatActivity() {                    // AppCompatActivity : 지원 라이브러리 테마 등 ui관련 호환성으로 구형 단말기에도 신규 api를 호환하게 해준다.

    private val viewModel: PlantListViewModel by viewModels()   // 내부 viewModelProvider를 사용하여 viewModel을 지연 생성한다.

    private val menuProvider = object : MenuProvider {          // 메뉴를 생성
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menu.clear()                                        // 메뉴를 초기화. 기존의 메뉴 아이템을 모두 제거
            menuInflater.inflate(R.menu.menu_plant_list, menu)  // XML 리소스 파일 (R.menu.menu_plant_list)을 사용하여 메뉴를 불러온다.?
        }

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {  // 메뉴 아이템이 선택되었을 때 처리하는 함수
            return when (menuItem.itemId) {
                R.id.filter_zone -> {                           // R.id.filter_zone 메뉴 아이템이 선택되면
                    viewModel.updateData()                      // ViewModel 객체의 updateData() 함수를 호출하여 데이터를 업데이트
                    true                                        // 이벤트를 처리한 후 true 리턴
                }

                else -> false                                   // 다른 메뉴 아이템이 선택될 때
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)  // 시스템 윈도우에 대한 시스템 패딩을 제거

        setContentView(ComposeView(this).apply {            // 화면의 내용을 구성하는 ComposeView를 생성하고 설정
            consumeWindowInsets = false                                           // WindowInsets를 소비하지 않도록 설정
            setContent {                                                          // Composable 컨텐츠를 설정
//                MaterialTheme {                                                 // Material Design 테마를 적용
                MdcTheme {                                                        // MdcTheme 테마 적용
                    SunflowerApp(                                                 // SunflowerApp Composable을 호출
                        onAttached = { toolbar ->                                 // 툴바가 액티비티에 추가되었을 때 실행할 작업을 정의
                            setSupportActionBar(toolbar)                          // 액티비티의 액션 바를 설정
                        },
                        onPageChange = { page ->                                  // 페이지 변경 이벤트를 처리하는 함수를 정의
                            when (page) {
                                SunflowerPage.MY_GARDEN -> removeMenuProvider(menuProvider)  // SunflowerPage.MY_GARDEN 페이지인 경우 메뉴 프로바이더를 제거
                                SunflowerPage.PLANT_LIST -> addMenuProvider(                 // SunflowerPage.PLANT_LIST 페이지인 경우 메뉴 프로바이더를 추가
                                    menuProvider,
                                    this@GardenActivity
                                )
                            }
                        },
                        plantListViewModel = viewModel,                            // 식물 목록 관련 뷰 모델을 설정
                    )
                }
            }
        })
    }
}