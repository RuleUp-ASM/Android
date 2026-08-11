package com.ruleup.observability.data.sink

import android.content.Context
import com.amplitude.android.Amplitude
import com.amplitude.android.Configuration
import com.amplitude.common.Logger
import com.ruleup.observability.domain.event.ObsEvent
import com.ruleup.observability.domain.model.AmplitudeApiKey
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.port.Sink

/**
 * Amplitude 출구. Firebase 와 **병행**한다 — 같은 이벤트가 두 곳에 쌓이므로 집계할 때 출처를 섞지 않는다.
 *
 * SDK 가 자체 큐·배치 전송을 가지므로 [FirebaseAnalyticsSink] 와 마찬가지로 **여기서 큐를 덧대지 않는다.**
 * `track` 은 즉시 리턴하고 실제 업로드는 나중에 일어나, 업로드 실패는 이 호출로 관측되지 않는다.
 *
 * **autocapture 는 세션만 켠다.** 화면 조회·요소 상호작용 자동 수집은 우리가 이름과 파라미터를 정해 둔
 * 이벤트와 별개로 쌓여, 대시보드에서 무엇이 우리 정의인지 구분이 안 되게 만든다. 화면 조회는 이미
 * `screen_view` 페이로드로 파이프라인을 타고 있다.
 *
 * [Amplitude] 인스턴스는 지연 생성한다 — 생성 시점에 SDK 가 저장소·네트워크를 건드리므로 첫 이벤트가
 * 나기 전까지 미룬다.
 */
internal class AmplitudeSink(
    private val context: Context,
    private val apiKey: AmplitudeApiKey,
    private val profile: BuildProfile,
) : Sink {
    private val amplitude by lazy {
        Amplitude(
            Configuration(
                apiKey = apiKey.value,
                context = context,
            ),
        ).also {
            // 개발 빌드에서만 SDK 내부 로그를 연다. 기본값(WARN)이면 전송 성공이 아무 흔적도 남기지
            // 않아 "올라가고 있나"를 확인할 방법이 없다. 프로덕션은 로그를 남기지 않는다.
            if (profile != BuildProfile.PRODUCTION) it.logger.logMode = Logger.LogMode.DEBUG
        }
    }

    override fun emit(event: ObsEvent) {
        amplitude.track(
            AmplitudeEventMapper.eventName(event.payload),
            AmplitudeEventMapper.toProperties(event),
        )
    }
}
