package com.ruleup.android_ruleup.tti

import com.lemonappdev.konsist.api.Konsist
import org.junit.Test
import kotlin.test.assertTrue

/**
 * TTI 를 여는 화면은 데이터 콘텐츠에 그리기 마커를 달아야 한다.
 * 마커가 없으면 VIEW_BINDING 이 닫히지 않아 그 화면의 TTI 가 한 건도 나가지 않는다.
 */
class TtiScreenCoverageTest {
    @Test
    fun `TtiScreenEffect 를 부르는 화면 패키지는 콘텐츠 그리기 마커도 단다`() {
        // 화면과 그 콘텐츠가 같은 패키지의 다른 파일로 나뉘어 있기도 하다
        val byPackage =
            Konsist
                .scopeFromProject()
                .files
                .filter { it.path.contains("/presentation/src/main/") && !it.path.contains("/tti/presentation/") }
                .groupBy { it.packagee?.name }

        val missing =
            byPackage
                .filterValues { files -> files.any { "TtiScreenEffect(" in it.text } }
                .filterValues { files -> files.none { ".ttiContentDrawn()" in it.text } }
                .keys

        assertTrue(missing.isEmpty(), "그리기 마커가 없는 화면 패키지: $missing")
    }
}
