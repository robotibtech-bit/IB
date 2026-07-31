package com.example.ibtech

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ibtech.data.temi.TemiRepository
import com.example.ibtech.ui.main.MainDashboardScreen
import com.example.ibtech.ui.main.TemiViewModel
import com.example.ibtech.ui.main.message
import com.example.ibtech.ui.theme.IBTECHTheme

class MainActivity : ComponentActivity() {

    private val temiRepository by lazy { TemiRepository.getInstance() }
    private val viewModel: TemiViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IBTECHTheme {
                val context = LocalContext.current
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val snackbarHostState = remember { SnackbarHostState() }
                val welcomeMessage = stringResource(R.string.welcome_message)
                val dismissLabel = stringResource(R.string.alert_action_dismiss)

                // 준비가 끝나면 한 번만 권한을 요청한다.
                LaunchedEffect(uiState.isReady, uiState.permissions.checked) {
                    viewModel.requestPermissionsOnce()
                }

                // 이동 실패·도착·오류 안내. 이전 안내는 즉시 밀어내 최신 상황만 남긴다.
                LaunchedEffect(Unit) {
                    viewModel.alerts.collect { alert ->
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = alert.message(context),
                            actionLabel = dismissLabel,
                            duration = SnackbarDuration.Long
                        )
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    snackbarHost = { SnackbarHost(snackbarHostState) }
                ) { innerPadding ->
                    MainDashboardScreen(
                        uiState = uiState,
                        onGoTo = { location -> viewModel.goTo(location) },
                        onSpeakWelcome = { viewModel.speak(welcomeMessage) },
                        onStop = { viewModel.stopMovement() },
                        onRefreshLocations = { viewModel.refreshLocations() },
                        onRequestPermissions = { viewModel.requestPermissions() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    // SDK 리스너 등록/해제는 반드시 대칭 쌍으로 유지한다.
    override fun onStart() {
        super.onStart()
        temiRepository.onStart(this)
    }

    override fun onResume() {
        super.onResume()
        // temi 설정 화면에서 권한을 바꾸고 돌아왔을 수 있으므로 다시 확인한다.
        viewModel.refreshPermissions()
    }

    override fun onStop() {
        temiRepository.onStop()
        super.onStop()
    }
}
