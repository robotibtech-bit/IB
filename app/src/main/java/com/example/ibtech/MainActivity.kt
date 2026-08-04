package com.example.ibtech

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ibtech.navigation.LibraryNavHost
import com.example.ibtech.robot.TemiRepository
import com.example.ibtech.ui.theme.IBTECHTheme

/**
 * 단일 Activity + Navigation Compose (요구사항 2.2절 권장 기술 기준).
 * 실제 화면 구성은 [LibraryNavHost] 그래프가 담당하고, 이 클래스는 temi SDK 생명주기
 * 연결([TemiRepository])만 책임진다.
 */
class MainActivity : ComponentActivity() {

    private val temiRepository by lazy { TemiRepository.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IBTECHTheme {
                LibraryNavHost()
            }
        }
    }

    // SDK 리스너 등록/해제는 반드시 대칭 쌍으로 유지한다.
    override fun onStart() {
        super.onStart()
        temiRepository.onStart(this)
    }

    override fun onStop() {
        temiRepository.onStop()
        super.onStop()
    }
}
