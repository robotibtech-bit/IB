package com.example.ibtech.navigation

import androidx.activity.compose.BackHandler
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
import com.example.ibtech.data.repository.StatsRepository
import com.example.ibtech.domain.model.LibrarySettings
import com.example.ibtech.domain.model.StatEventType
import com.example.ibtech.robot.TemiConnectionState
import com.example.ibtech.robot.TemiControllerProvider
import com.example.ibtech.ui.admin.AdminHomeScreen
import com.example.ibtech.ui.admin.AdminLoginScreen
import com.example.ibtech.ui.admin.AdminLoginViewModel
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
import com.example.ibtech.ui.common.LibraryScaffold
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
                val menuFindFacility = stringResource(R.string.home_action_find_facility)
                val menuUsageGuide = stringResource(R.string.home_action_usage_guide)
                val menuKidsContent = stringResource(R.string.home_action_kids_content)
                val menuTodayEvents = stringResource(R.string.home_action_today_events)

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
                            navController.navigate(LibraryRoutes.EVENTS)
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
                        onQrOpened = viewModel::onQrOpened,
                        onGoHome = { goHome() },
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
                            onQuizChoice1Change = viewModel::onQuizChoice1Change,
                            onQuizChoice2Change = viewModel::onQuizChoice2Change,
                            onQuizChoice3Change = viewModel::onQuizChoice3Change,
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
                            onAddEvent = viewModel::onAddEvent,
                            onEditEvent = viewModel::onEditEvent,
                            onDeleteEvent = viewModel::onDeleteEvent,
                            onAddNotice = viewModel::onAddNotice,
                            onEditNotice = viewModel::onEditNotice,
                            onDeleteNotice = viewModel::onDeleteNotice,
                            onDismissDialog = viewModel::onDismissDialog,
                            onEventTitleChange = viewModel::onEventTitleChange,
                            onEventStartDateChange = viewModel::onEventStartDateChange,
                            onEventEndDateChange = viewModel::onEventEndDateChange,
                            onEventTimeTextChange = viewModel::onEventTimeTextChange,
                            onEventPlaceChange = viewModel::onEventPlaceChange,
                            onEventTargetChange = viewModel::onEventTargetChange,
                            onEventDescriptionChange = viewModel::onEventDescriptionChange,
                            onEventQrUrlChange = viewModel::onEventQrUrlChange,
                            onEventFacilityChange = viewModel::onEventFacilityChange,
                            onEventEnabledChange = viewModel::onEventEnabledChange,
                            onEventSortOrderChange = viewModel::onEventSortOrderChange,
                            onSaveEventDraft = viewModel::onSaveEventDraft,
                            onNoticeTextChange = viewModel::onNoticeTextChange,
                            onNoticeEnabledChange = viewModel::onNoticeEnabledChange,
                            onNoticeSortOrderChange = viewModel::onNoticeSortOrderChange,
                            onSaveNoticeDraft = viewModel::onSaveNoticeDraft,
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
                            onFeaturedFacilityCountChange = viewModel::onFeaturedFacilityCountChange,
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
