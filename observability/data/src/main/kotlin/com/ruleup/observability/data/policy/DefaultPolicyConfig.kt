package com.ruleup.observability.data.policy

import com.ruleup.observability.domain.event.Channel
import com.ruleup.observability.domain.model.BuildProfile
import com.ruleup.observability.domain.model.Severity

/** 컴파일 기본 설정. */
internal fun defaultPolicyConfig(profile: BuildProfile): PolicyConfig =
    PolicyConfig.of(
        channelFloors =
            mapOf(
                Channel.DIAGNOSTIC to
                    when (profile) {
                        BuildProfile.DEV -> Severity.VERBOSE
                        BuildProfile.QA -> Severity.DEBUG
                        BuildProfile.PRODUCTION -> Severity.WARN
                    },
            ),
    )
