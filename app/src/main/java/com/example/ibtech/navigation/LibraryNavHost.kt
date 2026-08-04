package com.example.ibtech.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ibtech.BuildConfig
import com.example.ibtech.R
import com.example.ibtech.data.repository.SettingsRepository
import com.example.ibtech.domain.model.LibrarySettings
import com.example.ibtech.robot.TemiConnectionState
import com.example.ibtech.robot.TemiControllerProvider
import com.example.ibtech.ui.common.ConfirmDialog
import com.example.ibtech.ui.common.IdleTimeoutObserver
import com.example.ibtech.ui.common.LibraryScaffold
import com.example.ibtech.ui.common.PlaceholderScreen
import com.example.ibtech.ui.dev.DevMenuScreen
import com.example.ibtech.ui.dev.DevMenuViewModel
import com.example.ibtech.ui.facility.FacilityDetailScreen
import com.example.ibtech.ui.facility.FacilityDetailViewModel
import com.example.ibtech.ui.facility.FacilityListScreen
import com.example.ibtech.ui.facility.FacilityListViewModel
import com.example.ibtech.ui.facility.LocationMapScreen
import com.example.ibtech.ui.facility.LocationMapViewModel
import com.example.ibtech.ui.facility.NavigationProgressScreen
import com.example.ibtech.ui.facility.NavigationViewModel
import com.example.ibtech.ui.home.HomeScreen
import com.example.ibtech.ui.home.HomeViewModel
import com.example.ibtech.ui.usage.UsageAnswerScreen
import com.example.ibtech.ui.usage.UsageAnswerViewModel
import com.example.ibtech.ui.usage.UsageCategoryScreen
import com.example.ibtech.ui.usage.UsageCategoryViewModel
import com.example.ibtech.ui.usage.UsageSubcategoryScreen
import com.example.ibtech.ui.usage.UsageSubcategoryViewModel

/**
 * 요구사항 명세서 3절의 공통 규칙을 코드로 고정한다.
 * - `홈`은 항상 백스택을 비우고 [LibraryRoutes.HOME]으로 이동한다.
 * - `뒤로가기`는 NavController의 popBackStack 하나만 쓴다. 시스템 뒤로가기도 Compose Navigation이
 *   기본 제공하는 OnBackPressedCallback을 통해 동일한 경로를 타므로 별도 처리를 하지 않는다.
 *
 * 화면 어디를 터치하든(자식 버튼 포함) 최상위 [Box]가 [PointerEventPass.Initial] 단계에서
 * 먼저 이벤트를 관찰해 [interactionTick]을 올린다 — 클릭을 가로채지 않으면서 무입력 타이머만
 * 초기화한다(4.2절).
 */
@Composable
fun LibraryNavHost(navController: NavHostController = rememberNavController()) {
    val context = LocalContext.current
    val settingsRepository = remember { SettingsRepository.getInstance(context) }
    val settings by settingsRepository.settings.collectAsStateWithLifecycle(
        initialValue = LibrarySettings()
    )

    // remember로 고정하지 않는다 — 개발자 메뉴에서 Fake/Real을 토글하면 여기도 새 컨트롤러를
    // 즉시 관찰해야 무입력 복귀 판단(`isRobotBusy`)이 옛 컨트롤러에 머무르지 않는다.
    val temiController = TemiControllerProvider.current
    val navigationState by temiController.navigationState.collectAsStateWithLifecycle()
    val batteryStatus by temiController.batteryStatus.collectAsStateWithLifecycle()
    val connectionState by temiController.connectionState.collectAsStateWithLifecycle()
    val permissionStatus by temiController.permissionStatus.collectAsStateWithLifecycle()

    var interactionTick by remember { mutableIntStateOf(0) }
    val currentRoute by navController.currentBackStackEntryAsState()

    // temi가 처음 Ready가 되고 권한 확인이 끝났는데 MAP 권한이 없으면 한 번만 자동으로
    // 요청한다(6단계). 프로세스당 1회만 — 사용자가 거부했는데 화면을 옮길 때마다 팝업이 다시
    // 뜨면 안 되므로, 놓쳤거나 거부한 경우의 재시도는 시설 상세 화면의 수동 버튼으로만 한다.
    var hasAutoRequestedPermission by remember { mutableStateOf(false) }
    LaunchedEffect(connectionState, permissionStatus) {
        if (!hasAutoRequestedPermission &&
            connectionState is TemiConnectionState.Ready &&
            permissionStatus.checked &&
            permissionStatus.missing.isNotEmpty() &&
            !permissionStatus.requestInFlight
        ) {
            hasAutoRequestedPermission = true
            temiController.requestMissingPermissions()
        }
    }

    fun goHome() {
        navController.navigate(LibraryRoutes.HOME) {
            popUpTo(LibraryRoutes.HOME) { inclusive = true }
            launchSingleTop = true
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        interactionTick++
                    }
                }
            }
    ) {
        NavHost(navController = navController, startDestination = LibraryRoutes.HOME) {
            composable(LibraryRoutes.HOME) {
                val viewModel: HomeViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                if (uiState.isLoaded) {
                    HomeScreen(
                        welcomeMessage = uiState.settings.welcomeMessage,
                        onFindFacility = { navController.navigate(LibraryRoutes.facilityList()) },
                        onUsageGuide = { navController.navigate(LibraryRoutes.USAGE_CATEGORY) },
                        onKidsContent = { navController.navigate(LibraryRoutes.KIDS_MENU) },
                        onTodayEvents = { navController.navigate(LibraryRoutes.EVENTS) },
                        onDevMenuClick = if (BuildConfig.DEBUG) {
                            { navController.navigate(LibraryRoutes.DEV_MENU) }
                        } else {
                            null
                        }
                    )
                }
            }

            composable(
                route = LibraryRoutes.FACILITY_LIST,
                arguments = listOf(
                    navArgument("query") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                val viewModel: FacilityListViewModel = viewModel()
                val listUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_facility_list),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    FacilityListScreen(
                        uiState = listUiState,
                        onQueryChange = viewModel::onQueryChange,
                        onToggleSearch = viewModel::onToggleSearch,
                        onSelectFacility = { facility ->
                            navController.navigate(LibraryRoutes.facilityDetail(facility.id))
                        },
                        onGoHome = { goHome() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(
                route = LibraryRoutes.FACILITY_DETAIL,
                arguments = listOf(navArgument("facilityId") { type = NavType.StringType })
            ) { backStackEntry ->
                val facilityId = backStackEntry.arguments?.getString("facilityId").orEmpty()
                val viewModel: FacilityDetailViewModel = viewModel()
                val detailUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = detailUiState.facility?.name ?: stringResource(R.string.title_facility_list),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    FacilityDetailScreen(
                        uiState = detailUiState,
                        onEscortClick = {
                            navController.navigate(LibraryRoutes.facilityNavigation(facilityId))
                        },
                        onLocationOnlyClick = {
                            navController.navigate(LibraryRoutes.facilityMap(facilityId))
                        },
                        onGoHome = { goHome() },
                        onRequestPermission = viewModel::onRequestPermission,
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(
                route = LibraryRoutes.FACILITY_MAP,
                arguments = listOf(navArgument("facilityId") { type = NavType.StringType })
            ) {
                val viewModel: LocationMapViewModel = viewModel()
                val mapUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.facility_map_title),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    LocationMapScreen(
                        uiState = mapUiState,
                        onGoHome = { goHome() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(
                route = LibraryRoutes.FACILITY_NAVIGATION,
                arguments = listOf(navArgument("facilityId") { type = NavType.StringType })
            ) { backStackEntry ->
                val facilityId = backStackEntry.arguments?.getString("facilityId").orEmpty()
                val viewModel: NavigationViewModel = viewModel()
                val navUiState by viewModel.uiState.collectAsStateWithLifecycle()
                val isBusy = navUiState.navigationState.isBusy

                // 이동 중(Requested/Moving)일 때만 뒤로/홈을 바로 실행하지 않고 확인창을 띄운다
                // (요구사항 3장 예외). NavHost 레벨에서 처리하는 이유: 상단바 뒤로/홈 버튼은
                // 이 화면(NavigationProgressScreen) 바깥의 LibraryScaffold가 소유하기 때문이다.
                var pendingExit by remember { mutableStateOf<PendingExit?>(null) }

                fun retry() {
                    navController.popBackStack()
                    navController.navigate(LibraryRoutes.facilityNavigation(facilityId))
                }

                BackHandler(enabled = isBusy) { pendingExit = PendingExit.BACK }

                LibraryScaffold(
                    title = stringResource(R.string.title_facility_navigation),
                    onBack = {
                        if (isBusy) pendingExit = PendingExit.BACK else navController.popBackStack()
                    },
                    onHome = {
                        if (isBusy) pendingExit = PendingExit.HOME else goHome()
                    }
                ) { padding ->
                    NavigationProgressScreen(
                        uiState = navUiState,
                        onConfirmStart = viewModel::onConfirmStart,
                        onCancelStart = { navController.popBackStack() },
                        onStop = viewModel::onStopRequested,
                        onRetry = ::retry,
                        onLocationOnlyClick = {
                            navController.navigate(LibraryRoutes.facilityMap(facilityId))
                        },
                        onGoHome = { goHome() },
                        modifier = Modifier.padding(padding)
                    )
                }

                if (pendingExit != null) {
                    ConfirmDialog(
                        title = stringResource(R.string.navigation_exit_confirm_title),
                        body = stringResource(R.string.navigation_exit_confirm_body),
                        confirmLabel = stringResource(R.string.navigation_exit_confirm_action),
                        dismissLabel = stringResource(R.string.facility_detail_escort_confirm_cancel),
                        onConfirm = {
                            viewModel.onStopRequested()
                            when (pendingExit) {
                                PendingExit.BACK -> navController.popBackStack()
                                PendingExit.HOME -> goHome()
                                null -> Unit
                            }
                            pendingExit = null
                        },
                        onDismiss = { pendingExit = null }
                    )
                }
            }

            composable(LibraryRoutes.DEV_MENU) {
                val viewModel: DevMenuViewModel = viewModel()
                val devUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.dev_menu_title),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    DevMenuScreen(
                        uiState = devUiState,
                        onToggleFake = viewModel::onToggleFake,
                        onSelectOutcome = viewModel::onSelectOutcome,
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(LibraryRoutes.USAGE_CATEGORY) {
                val viewModel: UsageCategoryViewModel = viewModel()
                val categoryUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_usage_category),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    UsageCategoryScreen(
                        uiState = categoryUiState,
                        onSelectCategory = { item ->
                            val singleTopicId = item.singleAnswerTopicId
                            if (singleTopicId != null) {
                                navController.navigate(LibraryRoutes.usageAnswer(singleTopicId))
                            } else {
                                navController.navigate(LibraryRoutes.usageSubcategory(item.category.id))
                            }
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(
                route = LibraryRoutes.USAGE_SUBCATEGORY,
                arguments = listOf(navArgument("categoryId") { type = NavType.StringType })
            ) {
                val viewModel: UsageSubcategoryViewModel = viewModel()
                val subcategoryUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = subcategoryUiState.category?.title ?: stringResource(R.string.title_usage_category),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    UsageSubcategoryScreen(
                        uiState = subcategoryUiState,
                        onSelectTopic = { topic ->
                            navController.navigate(LibraryRoutes.usageAnswer(topic.id))
                        },
                        onGoHome = { goHome() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(
                route = LibraryRoutes.USAGE_ANSWER,
                arguments = listOf(navArgument("topicId") { type = NavType.StringType })
            ) {
                val viewModel: UsageAnswerViewModel = viewModel()
                val answerUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = answerUiState.topic?.title ?: stringResource(R.string.title_usage_category),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    UsageAnswerScreen(
                        uiState = answerUiState,
                        onRelatedFacilityClick = { facilityId ->
                            navController.navigate(LibraryRoutes.facilityDetail(facilityId))
                        },
                        onStaffHelpClick = viewModel::onStaffHelpClick,
                        onDismissStaffHelp = viewModel::onDismissStaffHelp,
                        onGoHome = { goHome() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(LibraryRoutes.KIDS_MENU) {
                LibraryScaffold(
                    title = stringResource(R.string.title_kids_menu),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    PlaceholderScreen(
                        description = stringResource(R.string.placeholder_kids_menu),
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(LibraryRoutes.EVENTS) {
                LibraryScaffold(
                    title = stringResource(R.string.title_events),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    PlaceholderScreen(
                        description = stringResource(R.string.placeholder_events),
                        modifier = Modifier.padding(padding)
                    )
                }
            }
        }
    }

    IdleTimeoutObserver(
        navController = navController,
        currentRoute = currentRoute?.destination?.route,
        idleTimeoutSeconds = settings.idleTimeoutSeconds,
        isRobotBusy = navigationState.isBusy,
        isCharging = batteryStatus?.isCharging == true,
        interactionTick = interactionTick
    )
}

/** `facility_navigation`에서 이동 중 뒤로/홈을 눌렀을 때 확인 후 수행할 동작. */
private enum class PendingExit { BACK, HOME }
