package com.example.ibtech.robot

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * ViewModel 이 의존하는 temi 제어 표면.
 *
 * 실제 구현은 [TemiRepository] 하나뿐이지만, `Robot.getInstance()` 가 실기에서만 동작하므로
 * 이 인터페이스를 두어 ViewModel 을 JVM 단위 테스트로 검증할 수 있게 한다.
 */
interface TemiController {

    val connectionState: StateFlow<TemiConnectionState>
    val navigationState: StateFlow<NavigationState>
    val speechState: StateFlow<SpeechState>
    val locations: StateFlow<List<String>>
    val permissionStatus: StateFlow<TemiPermissionStatus>
    val batteryStatus: StateFlow<BatteryStatus?>

    /** SDK 가 보고한 오류 코드 스트림. ([com.robotemi.sdk.exception.SdkException] 의 code) */
    val sdkErrors: SharedFlow<Int>

    fun goTo(location: String): Boolean
    fun stopMovement(): Boolean
    fun speak(text: String, showOnScreen: Boolean = false): Boolean
    fun cancelSpeech(): Boolean
    fun refreshLocations(): Boolean
    fun refreshBattery(): Boolean
    fun refreshPermissions(): Boolean
    fun requestMissingPermissions(): Boolean

    fun reportNavigationTimeout(target: String)
    fun reportStopTimeout()

    /** 권한 요청 결과 콜백이 오지 않을 때의 폴백. 요청 중 표시를 풀고 실제 승인 상태를 다시 읽는다. */
    fun reportPermissionRequestTimeout()
    fun consumeNavigationResult()
    fun consumeSpeechResult()
}
