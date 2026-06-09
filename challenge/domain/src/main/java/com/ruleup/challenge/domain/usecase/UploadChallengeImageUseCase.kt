package com.ruleup.challenge.domain.usecase

import com.ruleup.challenge.domain.ChallengeRepository
import dev.zacsweers.metro.Inject

/**
 * 챌린지 대표 이미지 업로드 유스케이스 (명세 3.9).
 *
 * 로컬 이미지 URI 를 서버에 업로드하고 영구 URL 을 반환한다.
 * 생성(3.2)·수정(3.4) 직전에 호출해, 반환된 URL 을 폼의 imageUrl 로 전달한다.
 * 실패는 예외([com.ruleup.network.dto.ApiException] 등)로 전파된다.
 */
class UploadChallengeImageUseCase
    @Inject
    constructor(
        private val challengeRepository: ChallengeRepository,
    ) {
        suspend operator fun invoke(imageUri: String): String = challengeRepository.uploadImage(imageUri)
    }
