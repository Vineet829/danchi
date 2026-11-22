package com.danchi.common;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronizationUtils;

class AfterCommitTest {

    @Test
    void failingActionDoesNotThrow() {
        Runnable failingMail = () -> {
            throw new IllegalStateException("SMTP unavailable");
        };
        assertThatCode(() -> AfterCommit.run(failingMail)).doesNotThrowAnyException();
        TransactionSynchronizationManager.initSynchronization();
        try {
            AfterCommit.run(failingMail);
            assertThatCode(TransactionSynchronizationUtils::triggerAfterCommit).doesNotThrowAnyException();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
