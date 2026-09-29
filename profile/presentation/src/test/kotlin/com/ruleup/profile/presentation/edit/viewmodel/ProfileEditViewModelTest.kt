package com.ruleup.profile.presentation.edit.viewmodel

import com.ruleup.domain.entity.category.Category
import com.ruleup.domain.entity.user.AccountStatus
import com.ruleup.domain.test.RecordingNavigationHelper
import com.ruleup.profile.domain.entity.NicknameCheck
import com.ruleup.profile.domain.entity.NicknameCheckReason
import com.ruleup.profile.domain.entity.Profile
import com.ruleup.profile.domain.entity.SanctionHistory
import com.ruleup.profile.presentation.fake.FakeAccountRepository
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

/** 프로필 편집. */
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

            assertEquals(listOf(ProfileEditEffect.ShowMessage("프로필을 저장하지 못했어요")), effects)
            assertEquals(0, nav.backCount)
        }

    @Test
    fun `뒤로 가기는 이동 없이 화면만 닫는다`() {
        val nav = RecordingNavigationHelper()

        viewModel(repo(), nav).onIntent(ProfileEditIntent.Back)

        assertEquals(1, nav.backCount)
        assertEquals(emptyList(), nav.routes)
    }

    @Test
    fun `사진을 골랐다가 나가면 서버 사진은 바뀌지 않는다`() =
        runTest {
            val repo = repo(imageUrl = "https://example.com/old.jpg")
            val viewModel = viewModel(repo)
            viewModel.onIntent(ProfileEditIntent.Load)

            viewModel.onIntent(ProfileEditIntent.PickImage("content://photos/1"))
            viewModel.onIntent(ProfileEditIntent.PickImage("content://photos/2"))

            assertEquals("content://photos/2", viewModel.uiState.value.imagePreviewUrl)
            assertEquals(
                "https://example.com/old.jpg",
                viewModel.uiState.value.profile
                    ?.profileImageUrl,
            )
            viewModel.onIntent(ProfileEditIntent.Back)
            assertEquals(listOf("getProfile", "getCategories"), repo.calls)
        }

    @Test
    fun `사진만 바꾸고 저장하면 마지막 선택 사진만 반영한다`() =
        runTest {
            val uploaded = mutableListOf<String>()
            val repo =
                repo(uploadImage = {
                    uploaded += it
                    "https://example.com/new.jpg"
                })
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(repo, nav)
            viewModel.onIntent(ProfileEditIntent.Load)
            viewModel.onIntent(ProfileEditIntent.PickImage("content://photos/1"))
            viewModel.onIntent(ProfileEditIntent.PickImage("content://photos/2"))

            viewModel.onIntent(ProfileEditIntent.Save)

            assertEquals(listOf("content://photos/2"), uploaded)
            assertTrue("updateProfile" !in repo.calls)
            assertEquals("https://example.com/new.jpg", viewModel.uiState.value.imagePreviewUrl)
            assertEquals(false, viewModel.uiState.value.imageChanged)
            assertEquals(1, nav.backCount)
        }

    @Test
    fun `기존 사진을 제거해도 저장하기 전에는 서버에서 삭제하지 않는다`() =
        runTest {
            val repo = repo(imageUrl = "https://example.com/old.jpg")
            val viewModel = viewModel(repo)
            viewModel.onIntent(ProfileEditIntent.Load)

            viewModel.onIntent(ProfileEditIntent.RemoveImage)

            assertEquals(null, viewModel.uiState.value.imagePreviewUrl)
            assertTrue("deleteProfileImage" !in repo.calls)
            viewModel.onIntent(ProfileEditIntent.Save)
            assertEquals(1, repo.calls.count { it == "deleteProfileImage" })
            assertEquals(
                null,
                viewModel.uiState.value.profile
                    ?.profileImageUrl,
            )
        }

    @Test
    fun `사진이 없던 사람이 선택을 취소하면 저장할 사진 변경이 없다`() =
        runTest {
            val repo = repo()
            val viewModel = viewModel(repo)
            viewModel.onIntent(ProfileEditIntent.Load)
            viewModel.onIntent(ProfileEditIntent.PickImage("content://photos/1"))
            viewModel.onIntent(ProfileEditIntent.RemoveImage)

            viewModel.onIntent(ProfileEditIntent.Save)

            assertEquals(false, viewModel.uiState.value.imageChanged)
            assertEquals(listOf("getProfile", "getCategories"), repo.calls)
        }

    @Test
    fun `사진 업로드가 실패하면 선택을 보존하고 다시 저장할 수 있다`() =
        runTest {
            var attempts = 0
            val repo =
                repo(uploadImage = {
                    attempts++
                    if (attempts == 1) error("통신 실패")
                    "https://example.com/new.jpg"
                })
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(repo, nav)
            viewModel.onIntent(ProfileEditIntent.Load)
            viewModel.onIntent(ProfileEditIntent.PickImage("content://photos/1"))

            viewModel.onIntent(ProfileEditIntent.Save)

            assertEquals(0, nav.backCount)
            assertEquals(false, viewModel.uiState.value.isSaving)
            assertEquals("content://photos/1", viewModel.uiState.value.imagePreviewUrl)
            viewModel.onIntent(ProfileEditIntent.Save)
            assertEquals(2, attempts)
            assertEquals(1, nav.backCount)
        }

    @Test
    fun `사진 삭제가 실패하면 제거 선택을 보존하고 다시 저장할 수 있다`() =
        runTest {
            var attempts = 0
            val repo =
                repo(
                    imageUrl = "https://example.com/old.jpg",
                    deleteImage = {
                        attempts++
                        if (attempts == 1) error("통신 실패")
                    },
                )
            val nav = RecordingNavigationHelper()
            val viewModel = viewModel(repo, nav)
            viewModel.onIntent(ProfileEditIntent.Load)
            viewModel.onIntent(ProfileEditIntent.RemoveImage)

            viewModel.onIntent(ProfileEditIntent.Save)

            assertEquals(0, nav.backCount)
            assertTrue(viewModel.uiState.value.removeImage)
            assertEquals(
                "https://example.com/old.jpg",
                viewModel.uiState.value.profile
                    ?.profileImageUrl,
            )
            viewModel.onIntent(ProfileEditIntent.Save)
            assertEquals(2, attempts)
            assertEquals(1, nav.backCount)
        }

    @Test
    fun `닉네임 검증이 실패하면 선택한 사진도 전송하지 않는다`() =
        runTest {
            val repo = repo(checkNickname = { NicknameCheck(true, false, NicknameCheckReason.DUPLICATED) })
            val viewModel = viewModel(repo)
            viewModel.onIntent(ProfileEditIntent.Load)
            viewModel.onIntent(ProfileEditIntent.ChangeNickname("새이름"))
            viewModel.onIntent(ProfileEditIntent.PickImage("content://photos/1"))

            viewModel.onIntent(ProfileEditIntent.Save)

            assertTrue("uploadProfileImage" !in repo.calls)
            assertEquals(false, viewModel.uiState.value.isSaving)
            assertEquals("content://photos/1", viewModel.uiState.value.imagePreviewUrl)
        }

    @Test
    fun `닉네임 저장 후 사진만 실패하면 재시도 때 닉네임을 다시 변경하지 않는다`() =
        runTest {
            var attempts = 0
            val repo =
                repo(
                    updateProfile = { profile().let { it.copy(user = it.user.copy(nickname = "새이름")) } },
                    uploadImage = {
                        attempts++
                        if (attempts == 1) error("통신 실패")
                        "https://example.com/new.jpg"
                    },
                )
            val viewModel = viewModel(repo)
            viewModel.onIntent(ProfileEditIntent.Load)
            viewModel.onIntent(ProfileEditIntent.ChangeNickname("새이름"))
            viewModel.onIntent(ProfileEditIntent.PickImage("content://photos/1"))

            viewModel.onIntent(ProfileEditIntent.Save)
            viewModel.onIntent(ProfileEditIntent.Save)

            assertEquals(1, repo.calls.count { it == "updateProfile" })
            assertEquals(2, attempts)
            assertEquals(false, viewModel.uiState.value.imageChanged)
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
        imageUrl: String? = null,
        uploadImage: ((String) -> String)? = { "https://example.com/new.jpg" },
        deleteImage: () -> Unit = {},
    ) = FakeProfileRepository(
        profile = { profile().let { it.copy(user = it.user.copy(profileImageUrl = imageUrl)) } },
        categories = categories,
        checkNickname = checkNickname,
        updateProfile = updateProfile,
        uploadImage = uploadImage,
        deleteImage = deleteImage,
    )

    private fun catalog() =
        com.ruleup.profile.domain.entity.CategoryCatalog(
            categories = Category.entries.toList(),
            maxSelectable = 6,
        )

    private fun profile() =
        Profile(
            user =
                com.ruleup.domain.entity.user
                    .User("u1", "지현", null),
            email = null,
            nicknameChangedAt = null,
            nicknameChangeableAfter = null,
            mannerTemperature = 36.5,
            interestCategories = listOf(Category.entries.first()),
            createdAt = "2026-01-01T00:00:00Z",
        )

    private fun viewModel(
        repo: FakeProfileRepository = repo(),
        nav: RecordingNavigationHelper = RecordingNavigationHelper(),
    ) = ProfileEditViewModel(
        profileRepository = repo,
        accountRepository = FakeAccountRepository(sanctions = { cleanSanctions }),
        navigationHelper = nav,
    )

    private val cleanSanctions =
        SanctionHistory(
            accountStatus = AccountStatus.ACTIVE,
            activeSanction = null,
            admin = emptyList(),
            auto = emptyList(),
        )
}
