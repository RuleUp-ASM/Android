package com.ruleup.remoteconfig.data

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

/** 원격 설정 갱신과 재시작이 같은 캐시 형식을 읽고 쓸 수 있어야 한다. */
class ConfigSnapshotSerializationTest {
    @Test
    fun `원격 설정을 저장했다가 읽으면 같은 실험 배정을 복원한다`() {
        val assignment = ExperimentAssignment("run-1", "a", "A")
        val snapshot = ConfigSnapshot(100L, "A", assignment)
        val storedJson = """{"fetchedAt":100,"value":"A","assignment":{"runId":"run-1","variant":"a","value":"A"}}"""

        assertEquals(storedJson, Json.encodeToString(snapshot))
        assertEquals(snapshot, Json.decodeFromString<ConfigSnapshot>(storedJson))
    }
}
