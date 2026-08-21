package com.example.ibtech.navigation

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
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
import com.example.ibtech.data.booksearch.BookSearchApi
import com.example.ibtech.data.repository.DefaultUsageContent
import com.example.ibtech.data.repository.SettingsRepository
import com.example.ibtech.data.repository.StatsRepository
import com.example.ibtech.domain.model.LibrarySettings
import com.example.ibtech.domain.model.StatEventType
import com.example.ibtech.robot.TemiConnectionState
import com.example.ibtech.robot.TemiControllerProvider
import com.example.ibtech.ui.admin.AdminHomeScreen
import com.example.ibtech.ui.admin.AdminLoginScreen
import com.example.ibtech.ui.admin.AdminLoginViewModel
import com.example.ibtech.ui.admin.AppUpdateScreen
import com.example.ibtech.ui.admin.AppUpdateViewModel
import com.example.ibtech.ui.admin.FacilityAdminEditScreen
import com.example.ibtech.ui.admin.FacilityAdminEditViewModel
import com.example.ibtech.ui.admin.EventAdminScreen
import com.example.ibtech.ui.admin.EventAdminViewModel
import com.example.ibtech.ui.admin.FacilityAdminScreen
import com.example.ibtech.ui.admin.FacilityAdminViewModel
import com.example.ibtech.ui.admin.KidsContentAdminScreen
import com.example.ibtech.ui.admin.KidsContentAdminViewModel
import com.example.ibtech.ui.admin.SettingsAdminScreen
import com.example.ibtech.ui.admin.SettingsAdminViewModel
import com.example.ibtech.ui.admin.StatisticsScreen
import com.example.ibtech.ui.admin.StatisticsViewModel
import com.example.ibtech.ui.admin.UsageInfoAdminScreen
import com.example.ibtech.ui.admin.UsageInfoAdminViewModel
import com.example.ibtech.ui.common.ConfirmDialog
import com.example.ibtech.ui.common.IdleTimeoutObserver
import com.example.ibtech.ui.booksearch.BookSearchScreen
import com.example.ibtech.ui.booksearch.ShelfNavigationScreen
import com.example.ibtech.ui.common.LibraryScaffold
import com.example.ibtech.ui.common.LibraryWebView
import com.example.ibtech.ui.theme.AdminTypography
import com.example.ibtech.ui.theme.SurfaceWhite
import com.example.ibtech.ui.dev.DevMenuScreen
import com.example.ibtech.ui.dev.DevMenuViewModel
import com.example.ibtech.ui.events.EventDetailScreen
import com.example.ibtech.ui.events.EventDetailViewModel
import com.example.ibtech.ui.events.EventsScreen
import com.example.ibtech.ui.events.EventsViewModel
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
import com.example.ibtech.ui.seatstatus.SeatStatusMenuScreen
import com.example.ibtech.ui.kids.BookRecommendationScreen
import com.example.ibtech.ui.kids.BookRecommendationViewModel
import com.example.ibtech.ui.kids.KidsMenuScreen
import com.example.ibtech.ui.kids.KidsMenuViewModel
import com.example.ibtech.ui.kids.LibraryEtiquetteScreen
import com.example.ibtech.ui.kids.LibraryEtiquetteViewModel
import com.example.ibtech.ui.kids.QuizCategoryScreen
import com.example.ibtech.ui.kids.QuizCategoryViewModel
import com.example.ibtech.ui.kids.QuizPlayScreen
import com.example.ibtech.ui.kids.QuizResultScreen
import com.example.ibtech.ui.kids.QuizResultViewModel
import com.example.ibtech.ui.kids.QuizViewModel
import com.example.ibtech.ui.usage.UsageAnswerScreen
import com.example.ibtech.ui.usage.UsageAnswerViewModel
import com.example.ibtech.ui.usage.UsageCategoryScreen
import com.example.ibtech.ui.usage.UsageCategoryViewModel
import com.example.ibtech.ui.usage.UsageSubcategoryScreen
import com.example.ibtech.ui.usage.UsageSubcategoryViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    // 11단계 통계: 순수 클릭/화면전환 지점(메뉴 선택, 시설 요청)은 담당 ViewModel이 없어
    // 여기서 직접 기록한다. 화면 안에서 상태 전환으로 일어나는 이벤트(이동 성공/실패 등)는
    // 해당 ViewModel이 기록한다.
    val statsRepository = remember { StatsRepository.getInstance(context) }
    val statsScope = rememberCoroutineScope()
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
    val knownLocations by temiController.locations.collectAsStateWithLifecycle()

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

    // 관리자 설정의 음량/음량 고정을 로봇에 반영한다. 연결 직후(Ready) 한 번, 이후 관리자가
    // 저장할 때마다(settings.volume/volumeLocked 변경) 다시 적용된다. SETTINGS 권한이 늦게
    // 승인되는 경우까지 커버하기 위해 permissionStatus도 키로 둔다 — 최초 시도가 권한 미승인으로
    // 조용히 실패해도, 승인 직후 재구성에서 다시 적용된다.
    LaunchedEffect(connectionState, permissionStatus, settings.volume, settings.volumeLocked) {
        if (connectionState is TemiConnectionState.Ready) {
            temiController.applyVolumeSettings(settings.volume, settings.volumeLocked)
        }
    }

    fun goHome() {
        navController.navigate(LibraryRoutes.HOME) {
            popUpTo(LibraryRoutes.HOME) { inclusive = true }
            launchSingleTop = true
        }
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        interactionTick++
                        // 화면 아무 곳이나 터치하면 소프트 키보드를 닫는다. 텍스트 필드 자체를
                        // 눌렀을 때도 여기서 먼저 닫힌 뒤 해당 필드의 Main 패스 처리에서 포커스를
                        // 다시 얻으며 키보드가 재표시되므로 정상적으로 입력 가능하다.
                        if (event.changes.any { it.pressed }) {
                            focusManager.clearFocus(force = true)
                            keyboardController?.hide()
                        }
                    }
                }
            }
    ) {
        NavHost(navController = navController, startDestination = LibraryRoutes.HOME) {
            composable(LibraryRoutes.HOME) {
                val viewModel: HomeViewModel = viewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                val menuBookSearch = stringResource(R.string.home_action_book_search)
                val menuFindFacility = stringResource(R.string.home_action_find_facility)
                val menuUsageGuide = stringResource(R.string.home_action_usage_guide)
                val menuKidsContent = stringResource(R.string.home_action_kids_content)
                val menuTodayEvents = stringResource(R.string.home_action_today_events)
                val menuSeatStatus = stringResource(R.string.home_action_seat_status)

                fun logMenuSelect(label: String) {
                    statsScope.launch { statsRepository.logEvent(StatEventType.MENU_SELECT, label) }
                }

                if (uiState.isLoaded) {
                    HomeScreen(
                        welcomeMessage = uiState.settings.welcomeMessage,
                        onFindFacility = {
                            logMenuSelect(menuFindFacility)
                            navController.navigate(LibraryRoutes.facilityList())
                        },
                        onUsageGuide = {
                            logMenuSelect(menuUsageGuide)
                            navController.navigate(LibraryRoutes.USAGE_CATEGORY)
                        },
                        onKidsContent = {
                            logMenuSelect(menuKidsContent)
                            navController.navigate(LibraryRoutes.KIDS_MENU)
                        },
                        onTodayEvents = {
                            logMenuSelect(menuTodayEvents)
                            navController.navigate(
                                LibraryRoutes.webView(url = settings.eventNoticeUrl, title = menuTodayEvents)
                            )
                        },
                        onSeatStatus = {
                            logMenuSelect(menuSeatStatus)
                            navController.navigate(LibraryRoutes.SEAT_STATUS_MENU)
                        },
                        onBookSearch = {
                            logMenuSelect(menuBookSearch)
                            navController.navigate(LibraryRoutes.BOOK_SEARCH)
                        },
                        onAdminClick = { navController.navigate(LibraryRoutes.ADMIN_LOGIN) }
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
                            // ESCORT_START는 여기서 기록하지 않는다 — 사용자가 확인 팝업에서
                            // 취소할 수 있으므로, 실제 goTo() 수락 시점(NavigationViewModel.
                            // onConfirmStart)에 1회만 기록한다(요구사항 4.3절).
                            detailUiState.facility?.name?.let { name ->
                                statsScope.launch {
                                    statsRepository.logEvent(StatEventType.FACILITY_REQUEST, name)
                                }
                            }
                            navController.navigate(LibraryRoutes.facilityNavigation(facilityId))
                        },
                        onLocationOnlyClick = {
                            detailUiState.facility?.name?.let { name ->
                                statsScope.launch {
                                    statsRepository.logEvent(StatEventType.FACILITY_REQUEST, name)
                                    statsRepository.logEvent(StatEventType.LOCATION_ONLY_START, name)
                                }
                            }
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
                        idleTimeoutSeconds = settings.idleTimeoutSeconds,
                        interactionTick = interactionTick,
                        onConfirmStart = viewModel::onConfirmStart,
                        onCancelStart = { navController.popBackStack() },
                        onStop = viewModel::onStopRequested,
                        onRetry = ::retry,
                        onLocationOnlyClick = {
                            navUiState.facility?.name?.let { name ->
                                statsScope.launch {
                                    statsRepository.logEvent(StatEventType.FACILITY_REQUEST, name)
                                    statsRepository.logEvent(StatEventType.LOCATION_ONLY_START, name)
                                }
                            }
                            navController.navigate(LibraryRoutes.facilityMap(facilityId))
                        },
                        onFindAnotherFacility = {
                            navController.navigate(LibraryRoutes.facilityList()) {
                                popUpTo(LibraryRoutes.HOME)
                            }
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
                            // 실시간 좌석 현황은 설명 화면 없이 바로 웹뷰를 보여준다 — 안내문 자체가
                            // 목적이 아니라 그 자리에서 바로 확인하는 게 목적인 항목이라, 다른
                            // 항목들과 달리 답변 화면(및 "바로가기" 버튼)을 한 단계 건너뛴다.
                            if (topic.id == DefaultUsageContent.READING_SEAT_STATUS_TOPIC_ID && !topic.qrUrl.isNullOrBlank()) {
                                navController.navigate(LibraryRoutes.webView(url = topic.qrUrl, title = topic.title))
                            } else {
                                navController.navigate(LibraryRoutes.usageAnswer(topic.id))
                            }
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
                        onOpenUrl = { url ->
                            navController.navigate(
                                LibraryRoutes.webView(url = url, title = answerUiState.topic?.title.orEmpty())
                            )
                        },
                        onStaffHelpClick = viewModel::onStaffHelpClick,
                        onDismissStaffHelp = viewModel::onDismissStaffHelp,
                        onGoHome = { goHome() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(LibraryRoutes.KIDS_MENU) {
                val viewModel: KidsMenuViewModel = viewModel()

                LibraryScaffold(
                    title = stringResource(R.string.title_kids_menu),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    KidsMenuScreen(
                        onQuizClick = { navController.navigate(LibraryRoutes.KIDS_QUIZ_CATEGORY) },
                        onBooksClick = { navController.navigate(LibraryRoutes.kidsBookRecommendation()) },
                        onEtiquetteClick = { navController.navigate(LibraryRoutes.KIDS_ETIQUETTE) },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(LibraryRoutes.KIDS_QUIZ_CATEGORY) {
                val viewModel: QuizCategoryViewModel = viewModel()
                val categoryUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_kids_quiz_category),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    QuizCategoryScreen(
                        uiState = categoryUiState,
                        onSelectCategory = { category ->
                            navController.navigate(LibraryRoutes.kidsQuizPlay(category))
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(
                route = LibraryRoutes.KIDS_QUIZ_PLAY,
                arguments = listOf(navArgument("category") { type = NavType.StringType })
            ) { backStackEntry ->
                val category = backStackEntry.arguments?.getString("category").orEmpty()
                val viewModel: QuizViewModel = viewModel()
                val quizUiState by viewModel.uiState.collectAsStateWithLifecycle()

                // 문제를 다 풀면(isFinished) 결과 화면으로 넘어간다 — ViewModel은 내비게이션을
                // 직접 하지 않고 상태만 노출한다(이 앱의 기존 관례).
                LaunchedEffect(quizUiState.isFinished) {
                    if (quizUiState.isFinished) {
                        navController.navigate(
                            LibraryRoutes.kidsQuizResult(
                                category = category,
                                correct = quizUiState.correctCount,
                                total = quizUiState.totalCount,
                                bookIds = quizUiState.correctRecommendedBookIds
                            )
                        ) {
                            popUpTo(LibraryRoutes.KIDS_QUIZ_PLAY) { inclusive = true }
                        }
                    }
                }

                LibraryScaffold(
                    title = category,
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    QuizPlayScreen(
                        uiState = quizUiState,
                        onSelectChoice = viewModel::onSelectChoice,
                        onGoHome = { goHome() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(
                route = LibraryRoutes.KIDS_QUIZ_RESULT,
                arguments = listOf(
                    navArgument("category") { type = NavType.StringType },
                    navArgument("correct") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("total") { type = NavType.IntType; defaultValue = 0 },
                    navArgument("bookIds") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) {
                val viewModel: QuizResultViewModel = viewModel()
                val resultUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_kids_quiz_result),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    QuizResultScreen(
                        uiState = resultUiState,
                        onGoToChildrenFacility = { facilityId ->
                            navController.navigate(LibraryRoutes.facilityDetail(facilityId))
                        },
                        onRetryQuiz = {
                            navController.navigate(LibraryRoutes.KIDS_QUIZ_CATEGORY) {
                                popUpTo(LibraryRoutes.KIDS_QUIZ_CATEGORY) { inclusive = true }
                            }
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(
                route = LibraryRoutes.KIDS_BOOK_RECOMMENDATION,
                arguments = listOf(
                    navArgument("ageGroup") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("topic") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) {
                val viewModel: BookRecommendationViewModel = viewModel()
                val bookUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_kids_book_recommendation),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    BookRecommendationScreen(
                        uiState = bookUiState,
                        onAnswerTasteQuestion = viewModel::onAnswerTasteQuestion,
                        onShowMoreBooks = viewModel::onShowMoreBooks,
                        onRestartTaste = viewModel::onRestartTaste,
                        onSelectAgeGroup = viewModel::onSelectAgeGroup,
                        onSelectTopic = viewModel::onSelectTopic,
                        onResetFilters = viewModel::onResetFilters,
                        onGoToChildrenFacility = { facilityId ->
                            navController.navigate(LibraryRoutes.facilityDetail(facilityId))
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(LibraryRoutes.KIDS_ETIQUETTE) {
                val viewModel: LibraryEtiquetteViewModel = viewModel()
                val etiquetteUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_kids_etiquette),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    LibraryEtiquetteScreen(
                        uiState = etiquetteUiState,
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(LibraryRoutes.EVENTS) {
                val viewModel: EventsViewModel = viewModel()
                val eventsUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_events),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    EventsScreen(
                        uiState = eventsUiState,
                        onSelectEvent = { eventId ->
                            navController.navigate(LibraryRoutes.eventDetail(eventId))
                        },
                        onGoHome = { goHome() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(
                route = LibraryRoutes.EVENT_DETAIL,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) {
                val viewModel: EventDetailViewModel = viewModel()
                val eventDetailUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = eventDetailUiState.event?.title ?: stringResource(R.string.title_events),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    EventDetailScreen(
                        uiState = eventDetailUiState,
                        onRelatedFacilityClick = { facilityId ->
                            navController.navigate(LibraryRoutes.facilityDetail(facilityId))
                        },
                        onOpenUrl = { url ->
                            navController.navigate(
                                LibraryRoutes.webView(url = url, title = eventDetailUiState.event?.title.orEmpty())
                            )
                        },
                        onQrOpened = viewModel::onQrOpened,
                        onGoHome = { goHome() },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(LibraryRoutes.BOOK_SEARCH) {
                BookSearchScreen(
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() },
                    onSelectBook = { hit ->
                        val shelf = hit.shelf
                        navController.navigate(
                            LibraryRoutes.shelfNavigation(
                                bookId = hit.bookId,
                                title = hit.title,
                                callNo = hit.callNo,
                                location = shelf?.locationName.orEmpty(),
                                shelfLabel = shelf?.shelfLabel.orEmpty(),
                                room = shelf?.room.orEmpty(),
                                floor = shelf?.floor,
                                estimated = shelf?.isEstimated == true
                            )
                        )
                    }
                )
            }

            composable(
                route = LibraryRoutes.SHELF_NAVIGATION,
                arguments = listOf(
                    navArgument("bookId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" },
                    navArgument("callNo") { type = NavType.StringType; defaultValue = "" },
                    navArgument("location") { type = NavType.StringType; defaultValue = "" },
                    navArgument("shelfLabel") { type = NavType.StringType; defaultValue = "" },
                    navArgument("room") { type = NavType.StringType; defaultValue = "" },
                    // 서버가 층을 모르면 빈 문자열이 온다. Int 인자로는 표현하지 못해 문자열로 받는다.
                    navArgument("floor") { type = NavType.StringType; defaultValue = "" },
                    navArgument("estimated") { type = NavType.StringType; defaultValue = "0" }
                )
            ) {
                ShelfNavigationScreen(
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                )
            }

            composable(LibraryRoutes.SEAT_STATUS_MENU) {
                val digitalRoomTitle = stringResource(R.string.seat_status_digital_room_title)
                val readingRoomTitle = stringResource(R.string.seat_status_reading_room_title)

                LibraryScaffold(
                    title = stringResource(R.string.title_seat_status_menu),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    SeatStatusMenuScreen(
                        onDigitalRoomStatus = {
                            navController.navigate(
                                LibraryRoutes.webView(
                                    url = DefaultUsageContent.DIGITAL_ROOM_RESERVATION_STATUS_URL,
                                    title = digitalRoomTitle
                                )
                            )
                        },
                        onReadingRoomSeatStatus = {
                            // 기존 "이용방법 → 실시간 좌석 현황"과 완전히 같은 URL·WebView
                            // 로직(WEB_VIEW 라우트, verticalScale 처리 포함)을 그대로 재사용한다.
                            navController.navigate(
                                LibraryRoutes.webView(
                                    url = DefaultUsageContent.READING_SEAT_STATUS_URL,
                                    title = readingRoomTitle
                                )
                            )
                        },
                        modifier = Modifier.padding(padding)
                    )
                }
            }

            composable(LibraryRoutes.ADMIN_LOGIN) {
                val viewModel: AdminLoginViewModel = viewModel()
                val loginUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(loginUiState.loginSuccess) {
                    if (loginUiState.loginSuccess) {
                        viewModel.onLoginHandled()
                        navController.navigate(LibraryRoutes.ADMIN_HOME) {
                            popUpTo(LibraryRoutes.ADMIN_LOGIN) { inclusive = true }
                        }
                    }
                }

                LibraryScaffold(
                    title = stringResource(R.string.title_admin_login),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = SurfaceWhite), typography = AdminTypography) {
                        AdminLoginScreen(
                            uiState = loginUiState,
                            onPasswordChange = viewModel::onPasswordChange,
                            onSubmit = viewModel::onSubmit,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }

            composable(LibraryRoutes.ADMIN_HOME) {
                LibraryScaffold(
                    title = stringResource(R.string.title_admin_home),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = SurfaceWhite), typography = AdminTypography) {
                        AdminHomeScreen(
                            onFacilityAdmin = { navController.navigate(LibraryRoutes.FACILITY_ADMIN) },
                            onUsageInfoAdmin = { navController.navigate(LibraryRoutes.USAGE_INFO_ADMIN) },
                            onKidsContentAdmin = { navController.navigate(LibraryRoutes.KIDS_CONTENT_ADMIN) },
                            onEventAdmin = { navController.navigate(LibraryRoutes.EVENT_ADMIN) },
                            onSettingsAdmin = { navController.navigate(LibraryRoutes.SETTINGS_ADMIN) },
                            onStatistics = { navController.navigate(LibraryRoutes.STATISTICS) },
                            onAppUpdate = { navController.navigate(LibraryRoutes.APP_UPDATE) },
                            onDevMenuClick = if (BuildConfig.DEBUG) {
                                { navController.navigate(LibraryRoutes.DEV_MENU) }
                            } else {
                                null
                            },
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }

            composable(LibraryRoutes.FACILITY_ADMIN) {
                val viewModel: FacilityAdminViewModel = viewModel()
                val facilityAdminUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_facility_admin),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = SurfaceWhite), typography = AdminTypography) {
                        FacilityAdminScreen(
                            uiState = facilityAdminUiState,
                            onRefresh = viewModel::onRefreshPoi,
                            onSelectFacility = { facility ->
                                navController.navigate(LibraryRoutes.facilityAdminEdit(facility.id))
                            },
                            onDeleteFacility = { facility -> viewModel.onDeleteFacility(facility.id) },
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }

            composable(
                route = LibraryRoutes.FACILITY_ADMIN_EDIT,
                arguments = listOf(navArgument("facilityId") { type = NavType.StringType })
            ) {
                val viewModel: FacilityAdminEditViewModel = viewModel()
                val editUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = editUiState.name.ifBlank { stringResource(R.string.title_facility_admin) },
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = SurfaceWhite), typography = AdminTypography) {
                        FacilityAdminEditScreen(
                            uiState = editUiState,
                            onNameChange = viewModel::onNameChange,
                            onFloorChange = viewModel::onFloorChange,
                            onDescriptionChange = viewModel::onDescriptionChange,
                            onGuideModeChange = viewModel::onGuideModeChange,
                            onDirectionChange = viewModel::onDirectionChange,
                            onNavigationTargetOverrideChange = viewModel::onNavigationTargetOverrideChange,
                            onIconKeyChange = viewModel::onIconKeyChange,
                            onEnabledChange = viewModel::onEnabledChange,
                            onFeaturedChange = viewModel::onFeaturedChange,
                            onSortOrderChange = viewModel::onSortOrderChange,
                            onSave = viewModel::onSave,
                            onSaved = { navController.popBackStack() },
                            onGoHome = { goHome() },
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }

            composable(LibraryRoutes.USAGE_INFO_ADMIN) {
                val viewModel: UsageInfoAdminViewModel = viewModel()
                val usageAdminUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_usage_info_admin),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = SurfaceWhite), typography = AdminTypography) {
                        UsageInfoAdminScreen(
                            uiState = usageAdminUiState,
                            onAddTopic = viewModel::onAddTopic,
                            onEditTopic = viewModel::onEditTopic,
                            onDeleteTopic = viewModel::onDeleteTopic,
                            onDismissDialog = viewModel::onDismissDialog,
                            onDraftTitleChange = viewModel::onDraftTitleChange,
                            onDraftShortAnswerChange = viewModel::onDraftShortAnswerChange,
                            onDraftTableDataChange = viewModel::onDraftTableDataChange,
                            onDraftQrUrlChange = viewModel::onDraftQrUrlChange,
                            onDraftFacilityChange = viewModel::onDraftFacilityChange,
                            onDraftEnabledChange = viewModel::onDraftEnabledChange,
                            onDraftSortOrderChange = viewModel::onDraftSortOrderChange,
                            onSaveDraft = viewModel::onSaveDraft,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }

            composable(LibraryRoutes.KIDS_CONTENT_ADMIN) {
                val viewModel: KidsContentAdminViewModel = viewModel()
                val kidsAdminUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_kids_content_admin),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = SurfaceWhite), typography = AdminTypography) {
                        KidsContentAdminScreen(
                            uiState = kidsAdminUiState,
                            onAddQuiz = viewModel::onAddQuiz,
                            onEditQuiz = viewModel::onEditQuiz,
                            onDeleteQuiz = viewModel::onDeleteQuiz,
                            onAddBook = viewModel::onAddBook,
                            onEditBook = viewModel::onEditBook,
                            onDeleteBook = viewModel::onDeleteBook,
                            onAddEtiquette = viewModel::onAddEtiquette,
                            onEditEtiquette = viewModel::onEditEtiquette,
                            onDeleteEtiquette = viewModel::onDeleteEtiquette,
                            onDismissDialog = viewModel::onDismissDialog,
                            onQuizCategoryChange = viewModel::onQuizCategoryChange,
                            onQuizQuestionChange = viewModel::onQuizQuestionChange,
                            onQuizChoiceChange = viewModel::onQuizChoiceChange,
                            onQuizChoiceCountChange = viewModel::onQuizChoiceCountChange,
                            onQuizCorrectIndexChange = viewModel::onQuizCorrectIndexChange,
                            onQuizExplanationChange = viewModel::onQuizExplanationChange,
                            onQuizToggleRecommendedBook = viewModel::onQuizToggleRecommendedBook,
                            onQuizEnabledChange = viewModel::onQuizEnabledChange,
                            onQuizSortOrderChange = viewModel::onQuizSortOrderChange,
                            onSaveQuizDraft = viewModel::onSaveQuizDraft,
                            onBookTitleChange = viewModel::onBookTitleChange,
                            onBookAuthorChange = viewModel::onBookAuthorChange,
                            onBookAgeGroupChange = viewModel::onBookAgeGroupChange,
                            onBookTopicChange = viewModel::onBookTopicChange,
                            onBookDescriptionChange = viewModel::onBookDescriptionChange,
                            onBookLocationChange = viewModel::onBookLocationChange,
                            onBookEnabledChange = viewModel::onBookEnabledChange,
                            onBookSortOrderChange = viewModel::onBookSortOrderChange,
                            onSaveBookDraft = viewModel::onSaveBookDraft,
                            onEtiquetteTextChange = viewModel::onEtiquetteTextChange,
                            onEtiquetteEnabledChange = viewModel::onEtiquetteEnabledChange,
                            onEtiquetteSortOrderChange = viewModel::onEtiquetteSortOrderChange,
                            onSaveEtiquetteDraft = viewModel::onSaveEtiquetteDraft,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }

            composable(LibraryRoutes.EVENT_ADMIN) {
                val viewModel: EventAdminViewModel = viewModel()
                val eventAdminUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_event_admin),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = SurfaceWhite), typography = AdminTypography) {
                        EventAdminScreen(
                            uiState = eventAdminUiState,
                            events = viewModel.events,
                            onNoticeUrlChange = viewModel::onNoticeUrlChange,
                            onSaveNoticeUrl = viewModel::onSaveNoticeUrl,
                            onResetNoticeUrl = viewModel::onResetNoticeUrl,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }

            composable(LibraryRoutes.SETTINGS_ADMIN) {
                val viewModel: SettingsAdminViewModel = viewModel()
                val settingsAdminUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_settings_admin),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = SurfaceWhite), typography = AdminTypography) {
                        SettingsAdminScreen(
                            uiState = settingsAdminUiState,
                            events = viewModel.events,
                            onWelcomeMessageChange = viewModel::onWelcomeMessageChange,
                            onIdleTimeoutChange = viewModel::onIdleTimeoutChange,
                            onBaseFloorChange = viewModel::onBaseFloorChange,
                            onVolumeChange = viewModel::onVolumeChange,
                            onVolumeLockedChange = viewModel::onVolumeLockedChange,
                            onFeaturedFacilityCountChange = viewModel::onFeaturedFacilityCountChange,
                            onBookSearchBaseUrlChange = viewModel::onBookSearchBaseUrlChange,
                            onSaveSettings = viewModel::onSaveSettings,
                            onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
                            onNewPasswordChange = viewModel::onNewPasswordChange,
                            onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                            onChangePassword = viewModel::onChangePassword,
                            onExportBackup = viewModel::onExportBackup,
                            onImportBackup = viewModel::onImportBackup,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }

            composable(LibraryRoutes.STATISTICS) {
                val viewModel: StatisticsViewModel = viewModel()
                val statisticsUiState by viewModel.uiState.collectAsStateWithLifecycle()

                LibraryScaffold(
                    title = stringResource(R.string.title_statistics),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = SurfaceWhite), typography = AdminTypography) {
                        StatisticsScreen(
                            uiState = statisticsUiState,
                            events = viewModel.events,
                            onSelectPeriod = viewModel::onSelectPeriod,
                            onExportCsv = viewModel::onExportCsv,
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }

            composable(LibraryRoutes.APP_UPDATE) {
                val viewModel: AppUpdateViewModel = viewModel()
                val appUpdateUiState by viewModel.uiState.collectAsStateWithLifecycle()
                val installPermissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.StartActivityForResult()
                ) { viewModel.refreshInstallPermission() }

                LibraryScaffold(
                    title = stringResource(R.string.title_app_update),
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    MaterialTheme(colorScheme = MaterialTheme.colorScheme.copy(background = SurfaceWhite), typography = AdminTypography) {
                        AppUpdateScreen(
                            uiState = appUpdateUiState,
                            onCheckForUpdate = viewModel::checkForUpdate,
                            onOpenInstallPermission = { installPermissionLauncher.launch(viewModel.installPermissionIntent()) },
                            onDownloadAndInstall = viewModel::downloadAndInstall,
                            onInstallApk = { context.startActivity(it) },
                            modifier = Modifier.padding(padding)
                        )
                    }
                }
            }

            composable(
                route = LibraryRoutes.WEB_VIEW,
                arguments = listOf(
                    navArgument("url") { type = NavType.StringType; defaultValue = "" },
                    navArgument("title") { type = NavType.StringType; defaultValue = "" }
                )
            ) { backStackEntry ->
                val url = backStackEntry.arguments?.getString("url").orEmpty()
                val webTitle = backStackEntry.arguments?.getString("title").orEmpty()

                // 뒤로가기(시스템·상단바 모두)는 웹페이지 자체 히스토리를 되짚지 않고 항상 바로
                // 이전 앱 화면으로 돌아간다 — 웹뷰 안에서 링크를 몇 번 타고 들어갔든 뒤로가기 한 번에
                // 키오스크 화면으로 복귀해야 한다(요구사항 3장 "뒤로가기는 popBackStack 하나만").
                LibraryScaffold(
                    title = webTitle.ifBlank { stringResource(R.string.web_view_default_title) },
                    onBack = { navController.popBackStack() },
                    onHome = { goHome() }
                ) { padding ->
                    LibraryWebView(
                        url = url,
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize(),
                        // 좌석현황 페이지는 가로로 넓게 설계돼 있어 가로로 긴 키오스크 화면에서도
                        // 위아래로 짧고 빈약해 보인다 — 세로 방향으로만 시각적으로 늘려 채운다.
                        verticalScale = if (url == DefaultUsageContent.READING_SEAT_STATUS_URL) 1.3f else 1f,
                        // 디지털자료실 예약현황(LibMate)은 고정 폭으로 만들어진 옛날 페이지라 넓은
                        // 키오스크 화면에서 오른쪽에 빈 여백이 크게 남는다 — 좌우 10%씩만 남기고
                        // 가운데 80%를 채우도록 가로로 늘려 가운데 정렬한다.
                        horizontalFit = url == DefaultUsageContent.DIGITAL_ROOM_RESERVATION_STATUS_URL
                    )
                }
            }
        }
    }

    // 도서검색 서버 예열.
    //
    // 서버(Cloud Run)를 min-instances=0 으로 두어 요청이 없으면 인스턴스가 내려간다. 다시
    // 깨어나는 데 40초쯤 걸리는데(이미지 4GB + 임베딩 모델 적재), 앱의 읽기 타임아웃은
    // 10초라 잠든 서버에 검색을 걸면 "연결하지 못했습니다"로 끝난다. 로봇이 켜져 있는 동안
    // 주기적으로 깨워 두면 손님이 기다릴 일이 없다.
    //
    // NavHost 수준에 두는 이유: 화면과 무관하게 돌아야 한다. 홈 화면에 두면 퀴즈처럼 오래
    // 머무는 화면에서 코루틴이 취소되고, 하필 그 직후(퀴즈 → 책 추천)에 검색이 느려진다.
    //
    // 비용은 사실상 0원이다. 5분 간격이면 하루 300회 남짓이고 Cloud Run 무료 한도의 1%도
    // 쓰지 않는다. 유휴 인스턴스는 요청을 처리하는 동안만 과금되기 때문이다.
    //
    // 시연처럼 한 번도 실패하면 안 되는 자리에서는 서버를 아예 상시 대기로 올리는 편이
    // 확실하다. 인스턴스 교체나 동시 접속으로 예열이 뚫리는 경우까지 막아 준다.
    //
    //   gcloud run services update iblib-search --region=asia-northeast3 --min-instances=1
    //
    // 끝나면 반드시 되돌린다. 상시 대기는 하루 약 5,300원, 한 달이면 16만원이다.
    //
    //   gcloud run services update iblib-search --region=asia-northeast3 --min-instances=0
    //
    // 그때도 이 예열은 그대로 두면 된다. 서로 방해하지 않는다.
    LaunchedEffect(settings.bookSearchBaseUrl) {
        val baseUrl = settings.bookSearchBaseUrl
        if (baseUrl.isBlank()) return@LaunchedEffect
        val api = BookSearchApi()
        while (true) {
            // 실패는 삼킨다. 사용자를 위한 요청이 아니라 준비 작업이라, 서버가 죽었거나
            // 와이파이가 끊겨도 다른 기능을 쓰는 화면에 오류를 띄우면 안 된다.
            runCatching { api.health(baseUrl) }
            delay(SEARCH_WARMUP_INTERVAL_MILLIS)
        }
    }

    IdleTimeoutObserver(
        navController = navController,
        currentRoute = currentRoute?.destination?.route,
        idleTimeoutSeconds = settings.idleTimeoutSeconds,
        isRobotBusy = navigationState.isBusy,
        isCharging = batteryStatus?.isCharging == true,
        interactionTick = interactionTick,
        onIdleTimeout = {
            // 지도에 "홈" POI가 아직 없는 현장에서는 조용히 건너뛴다 — 화면 홈 복귀는 이미
            // 위에서 처리했으므로 이 콜백이 아무 것도 안 해도 사용자에게 문제가 되지 않는다.
            if (HOME_POI_NAME in knownLocations) {
                temiController.goTo(HOME_POI_NAME)
            }
        }
    )
}

/** 무입력 자동 복귀 시 로봇이 실제로 이동해 갈 POI 이름. 현장에서 이 이름으로 지도에 위치를
 * 등록해 두어야 동작한다(관리자 시설 등록과 무관한, 로봇 자체의 대기 위치). */
private const val HOME_POI_NAME = "홈"

/**
 * 도서검색 서버 예열 간격.
 *
 * Cloud Run이 유휴 인스턴스를 얼마나 붙잡아 두는지는 보장하지 않는다. 대체로 15분쯤이지만
 * 더 짧을 수 있어 5분으로 잡았다. 늘리려면 실제로 잠드는지 확인하고 올려야 한다.
 */
private const val SEARCH_WARMUP_INTERVAL_MILLIS = 5 * 60 * 1000L

/** `facility_navigation`에서 이동 중 뒤로/홈을 눌렀을 때 확인 후 수행할 동작. */
private enum class PendingExit { BACK, HOME }
