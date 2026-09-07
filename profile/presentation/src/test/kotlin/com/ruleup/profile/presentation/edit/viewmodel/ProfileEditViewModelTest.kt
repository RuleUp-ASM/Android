package com.ruleup.profile.presentation.edit.viewmodel

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.NicknameCheck
import com.ruleup.profile.domain.entity.NicknameCheckReason
import com.ruleup.profile.domain.entity.Profile
import com.ruleup.profile.presentation.fake.FakeProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 프로필 편집. 저장은 되돌리기 어려운 동작이라 **막을 것을 서버 왕복 전에 막고**, 저장이 실패했으면
 * 화면을 떠나지 않는 것이 계약이다 — 실패했는데 뒤로 가면 사용자는 저장된 줄 안다.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ProfileEditViewModelTest {
    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `불러오면 지금 프로필을 편집 상태로 올린다`() =
        runTest {
            val viewModel = viewModel(repo())

            viewModel.onIntent(ProfileEditIntent.Load)

            assertEquals("지현", viewModel.uiState.value.nickname)
        }

    @Test
    fun `관심 분야 마스터 조회가 실패해도 편집을 막지 않는다`() =
        runTest {
            // 상한만 못 받은 것이라 기본값으로 흡수한다 — 이걸로 화면 전체를 못 열면 과하다.
            val viewModel = viewModel(repo(categories = { throw IllegalStateException("마스터 오류") }))

            viewModel.onIntent(ProfileEditIntent.Load)

            assertEquals("지현", viewModel.uiState.value.nickname)
            assertTrue(viewModel.uiState.value.maxSelectable > 0)
        }

    @Test
    fun `바꾼 게 없으면 저장하지 않는다`() =
        runTest {
            val repo = repo()
            val viewModel = viewModel(repo)
            val effects = collectEffects(viewModel)
            viewModel.onIntent(ProfileEditIntent.Load)

            viewModel.onIntent(ProfileEditIntent.Save)

            assertEquals(listOf(ProfileEditEffect.ShowMessage("변경된 내용이 없어요")), effects)
            assertTrue(repo.calls.none { it == "updateProfile" })
        }

    @Test
    fun `관심 분야를 모두 지우면 저장하지 않는다`() =
        runTest {
            val repo = repo()
            val viewModel = viewModel(repo)
            viewModel.onIntent(ProfileEditIntent.Load)
            viewModel.onIntent(ProfileEditIntent.ToggleCategory(Category.entries.first()))

            viewModel.onIntent(ProfileEditIntent.Save)

            assertTrue(repo.calls.none { it == "updateProfile" })
        }

    @Test
    fun `이미 쓰는 닉네임이면 저장하지 않고 화면에 남는다`() =
        runTest {
            // 여기서 뒤로 가 버리면 사용자는 바뀐 줄 안다.
            val nav = RecordingNavigationHelper()
            val repo =
                repo(
                    checkNickname = { NicknameCheck(valid = true, available = false, reason = NicknameCheckReason.DUPLICATED) },
                )
            val viewModel = viewModel(repo, nav)
            val effects = collectEffects(viewModel)
            viewModel.onIntent(ProfileEditIntent.Load)
            viewModel.onIntent(ProfileEditIntent.ChangeNickname("새이름"))

            viewModel.onIntent(ProfileEditIntent.Save)

            assertEquals(listOf(ProfileEditEffect.ShowMessage("이미 사용 중인 닉네임이에요")), effects)
            assertTrue(repo.calls.none { it == "updateProfile" })
            assertTrue(nav.didNotMove)
        }

    @Test
    fun `최근 해제된 닉네임이면 언제부터 쓸 수 있는지 함께 알린다`() =
        runTest {
            val repo =
                repo(
                    checkNickname = {
                        NicknameCheck(
                            valid = true,
                            available = false,
                            reason = NicknameCheckReason.RECENTLY_RELEASED,
                            availableAt = "2026-09-08",
                        )
                    },
                )
            val viewModel = viewModel(repo)
            val effects = collectEffects(viewModel)
            viewModel.onIntent(ProfileEditIntent.Load)
            viewModel.onIntent(ProfileEditIntent.ChangeNickname("새이름"))

            viewModel.onIntent(ProfileEditIntent.Save)

            assertTrue(effects.single().let { it is ProfileEditEffect.ShowMessage && it.message.contains("2026-09-08") })
        }

    @Test
    fun `저장에 성공하면 알리고 화면을 떠난다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(repo(), nav)
            val effects = collectEffects(viewModel)
            viewModel.onIntent(ProfileEditIntent.Load)
            viewModel.onIntent(ProfileEditIntent.ChangeNickname("새이름"))

            viewModel.onIntent(ProfileEditIntent.Save)

            assertEquals(listOf(ProfileEditEffect.ShowMessage("프로필을 저장했어요")), effects)
            assertEquals(1, nav.backCount)
        }

    @Test
    fun `저장에 실패하면 화면을 떠나지 않는다`() =
        runTest {
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(repo(updateProfile = { throw IllegalStateException("저장 실패") }), nav)
            val effects = collectEffects(viewModel)
            viewModel.onIntent(ProfileEditIntent.Load)
            viewModel.onIntent(ProfileEditIntent.ChangeNickname("새이름"))

            viewModel.onIntent(ProfileEditIntent.Save)

            assertEquals(listOf(ProfileEditEffect.ShowMessage("저장 실패")), effects)
            assertEquals(0, nav.backCount)
        }

    @Test
    fun `뒤로 가기는 이동 없이 화면만 닫는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(repo(), nav).onIntent(ProfileEditIntent.Back)

        assertEquals(1, nav.backCount)
        assertEquals(emptyList(), nav.routes)
    }

    private fun TestScope.collectEffects(viewModel: ProfileEditViewModel): List<ProfileEditEffect> {
        val effects = mutableListOf<ProfileEditEffect>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.effect.toList(effects) }
        return effects
    }

    private fun repo(
        categories: (() -> com.ruleup.profile.domain.entity.CategoryCatalog)? = { catalog() },
        checkNickname: ((String) -> NicknameCheck)? = { NicknameCheck(valid = true, available = true, reason = null) },
        updateProfile: (() -> Profile)? = { profile() },
    ) = FakeProfileRepository(
        profile = { profile() },
        categories = categories,
        checkNickname = checkNickname,
        updateProfile = updateProfile,
    )

    private fun catalog() =
        com.ruleup.profile.domain.entity.CategoryCatalog(
            categories = Category.entries.toList(),
            maxSelectable = 6,
        )

    private fun profile() =
        Profile(
            id = "u1",
            nickname = "지현",
            email = null,
            profileImageUrl = null,
            nicknameChangedAt = null,
            nicknameChangeableAfter = null,
            mannerTemperature = 36.5,
            interestCategories = listOf(Category.entries.first()),
            createdAt = "2026-01-01T00:00:00Z",
        )

    private fun viewModel(
        repo: FakeProfileRepository = repo(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = ProfileEditViewModel(profileRepository = repo, navigationHelper = nav)
}
