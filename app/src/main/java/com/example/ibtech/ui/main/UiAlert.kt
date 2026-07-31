package com.example.ibtech.ui.main

import android.content.Context
import com.example.ibtech.R
import com.example.ibtech.data.temi.NavigationIssue

/**
 * 사용자에게 한 번만 보여줄 안내. 상태(State)가 아니라 사건(Event)이다.
 *
 * 문구를 담지 않고 종류만 담는다. 실제 문자열은 [UiAlert.message] 에서 `strings.xml` 로 매핑한다.
 */
sealed interface UiAlert {

    /** 목적지 도착. */
    data class Arrived(val target: String) : UiAlert

    /** 이동이 중단되거나 실패함. */
    data class NavigationFailed(val issue: NavigationIssue) : UiAlert

    /** 요청한 위치가 temi 지도에 없음. */
    data class DestinationUnreachable(val location: String) : UiAlert

    /** 로봇이 아직 준비되지 않았거나 SDK 호출이 거부됨. */
    data object RobotNotReady : UiAlert

    /** 지도 권한이 없어 이동할 수 없음. */
    data object PermissionRequired : UiAlert

    /** 권한 요청이 거부됨. */
    data object PermissionDenied : UiAlert

    /** 음성 안내 실패. */
    data object SpeechFailed : UiAlert

    /** SDK 가 보고한 오류. */
    data class SdkError(val code: Int) : UiAlert
}

/**
 * 안내 문구를 만든다.
 *
 * Compose 밖(코루틴 안)에서 스낵바를 띄우므로 `stringResource` 대신 [Context] 를 쓴다.
 */
fun UiAlert.message(context: Context): String = when (this) {
    is UiAlert.Arrived ->
        context.getString(R.string.nav_arrived_format, target)

    is UiAlert.NavigationFailed -> context.getString(
        when (issue) {
            NavigationIssue.USER_STOPPED -> R.string.nav_issue_user_stopped
            NavigationIssue.EXTERNAL_INTERRUPTION -> R.string.nav_issue_external
            NavigationIssue.TIMEOUT -> R.string.nav_issue_timeout
            NavigationIssue.SDK_ERROR -> R.string.nav_issue_error
        }
    )

    is UiAlert.DestinationUnreachable ->
        context.getString(R.string.alert_destination_unreachable_format, location)

    UiAlert.RobotNotReady ->
        context.getString(R.string.alert_robot_not_ready)

    UiAlert.PermissionRequired ->
        context.getString(R.string.alert_permission_required)

    UiAlert.PermissionDenied ->
        context.getString(R.string.alert_permission_denied)

    UiAlert.SpeechFailed ->
        context.getString(R.string.speech_failed)

    is UiAlert.SdkError ->
        context.getString(R.string.alert_sdk_error_format, code)
}
