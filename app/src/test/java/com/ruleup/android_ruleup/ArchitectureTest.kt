package com.ruleup.android_ruleup

import com.lemonappdev.konsist.api.Konsist
import com.lemonappdev.konsist.api.ext.list.withNameEndingWith
import com.lemonappdev.konsist.api.verify.assertFalse
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.Test

/**
 * 모듈 레이어 규칙(헥사고날) 아키텍처 테스트.
 *
 * - domain: 순수 코틀린. Android/안쪽 어댑터를 모른다.
 * - data / presentation: domain 에만 의존하는 어댑터. 서로를 모른다.
 * - 포트 구현체(…RepositoryImpl)는 어댑터 모듈에만 둔다.
 */
class ArchitectureTest {
    @Test
    fun `domain 모듈은 Android 프레임워크를 import 하지 않는다`() {
        Konsist
            .scopeFromProject()
            .files
            .filter { it.path.contains("/domain/src/") }
            .assertFalse { file ->
                file.imports.any { it.name.startsWith("android.") || it.name.startsWith("androidx.") }
            }
    }

    @Test
    fun `domain 모듈은 data·presentation 레이어를 import 하지 않는다`() {
        Konsist
            .scopeFromProject()
            .files
            .filter { it.path.contains("/domain/src/") }
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.startsWith("com.ruleup") &&
                        (import.name.contains(".data.") || import.name.contains(".presentation."))
                }
            }
    }

    @Test
    fun `presentation 모듈은 data 레이어를 import 하지 않는다`() {
        Konsist
            .scopeFromProject()
            .files
            .filter { it.path.contains("/presentation/src/") }
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.startsWith("com.ruleup") &&
                        (import.name.contains(".data.") || import.name.startsWith("com.ruleup.datastore"))
                }
            }
    }

    @Test
    fun `data 모듈은 presentation 레이어를 import 하지 않는다`() {
        Konsist
            .scopeFromProject()
            .files
            .filter { it.path.contains("/data/src/") }
            .assertFalse { file ->
                file.imports.any { import ->
                    import.name.startsWith("com.ruleup") && import.name.contains(".presentation.")
                }
            }
    }

    @Test
    fun `Repository 구현체는 data 계열(어댑터) 모듈에만 둔다`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withNameEndingWith("RepositoryImpl")
            .assertTrue { it.resideInPath("..data..") || it.resideInPath("..datastore..") }
    }

    @Test
    fun `UseCase 는 domain 모듈에만 둔다`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withNameEndingWith("UseCase")
            .assertTrue { it.resideInPath("..domain..") }
    }

    @Test
    fun `ViewModel 은 Repository 포트를 직접 주입받지 않고 UseCase 를 거친다`() {
        Konsist
            .scopeFromProject()
            .classes()
            .withNameEndingWith("ViewModel")
            .assertFalse { viewModel ->
                viewModel.primaryConstructor
                    ?.parameters
                    .orEmpty()
                    .any { it.type.name.endsWith("Repository") }
            }
    }
}
