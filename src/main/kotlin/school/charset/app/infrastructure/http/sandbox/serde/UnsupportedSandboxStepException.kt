package school.charset.app.infrastructure.http.sandbox.serde

import school.charset.app.domain.exercise.StepType

class UnsupportedSandboxStepException(stepType: StepType) : IllegalStateException("Step type $stepType is not part of the sandbox UTF-8 encode wire format")
