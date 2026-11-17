
package com.selfmind.app

enum class SensitiveAction {
    READ_PERSONAL_FILE,
    EDIT_PERSONAL_FILE,
    DELETE_FILE,
    READ_CONTACTS,
    SEND_MESSAGE,
    SHARE_DATA,
    ACCESS_MICROPHONE,
    ACCESS_CAMERA
}

enum class ApprovalStatus {
    APPROVED,
    DENIED,
    PENDING
}

object PermissionManager {

    private val approvals =
        mutableMapOf<SensitiveAction, ApprovalStatus>()

    @Synchronized
    fun requestApproval(action: SensitiveAction): ApprovalStatus {
        return approvals[action] ?: ApprovalStatus.PENDING
    }

    @Synchronized
    fun approve(action: SensitiveAction) {
        approvals[action] = ApprovalStatus.APPROVED
    }

    @Synchronized
    fun deny(action: SensitiveAction) {
        approvals[action] = ApprovalStatus.DENIED
    }

    @Synchronized
    fun consumeApproval(action: SensitiveAction): Boolean {
        val approved =
            approvals[action] == ApprovalStatus.APPROVED

        approvals.remove(action)

        return approved
    }

    @Synchronized
    fun reset(action: SensitiveAction) {
        approvals.remove(action)
    }

    @Synchronized
    fun resetAll() {
        approvals.clear()
    }

    @Synchronized
    fun canExecute(action: SensitiveAction): Boolean {
        return approvals[action] == ApprovalStatus.APPROVED
    }
}
