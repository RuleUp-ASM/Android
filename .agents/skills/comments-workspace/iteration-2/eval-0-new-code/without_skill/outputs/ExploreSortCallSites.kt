// 폴백을 enum 안으로 옮기면 따라오는 호출부·테스트 변경. 각 파일의 해당 줄만 바꾼다.

// ── 1) challenge/domain/src/test/.../entity/ExploreTest.kt
//    기존 `모르는 정렬 값은 null 이다` 테스트를 아래로 교체한다.
//    (import 도 assertNull → 불필요, assertEquals 는 이미 있음)

//    @Test
//    fun `모르는 정렬 값은 기본 정렬로 떨어진다`() {
//        // 구 TEMPLATE_USAGE 딥링크가 남아 있어도 화면은 인기순으로 열린다.
//        assertEquals(ExploreSort.POPULAR, ExploreSort.fromValue("TEMPLATE_USAGE"))
//        assertEquals(ExploreSort.POPULAR, ExploreSort.fromValue(null))
//    }

// ── 2) challenge/presentation/src/main/.../explore/list/viewmodel/ExploreListViewModel.kt:121
//    폴백이 enum 으로 올라갔으니 호출부의 엘비스는 지운다.

//    val initialSort = ExploreSort.fromValue(sort)
