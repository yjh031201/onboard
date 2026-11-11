package com.kanban.backend.integration.slack;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** Slack 알림은 저장(커밋)이 확정된 뒤에만, 호출한 쪽을 붙잡지 않고 보낸다. */
class SlackNotifierTest {

    private SlackIntegrationService slack;
    private SlackNotifier notifier;

    @BeforeEach
    void setUp() {
        slack = mock(SlackIntegrationService.class);
        notifier = new SlackNotifier(slack);
    }

    @AfterEach
    void tearDown() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        notifier.shutdown();
    }

    private List<TransactionSynchronization> beginTransaction() {
        TransactionSynchronizationManager.initSynchronization();
        return List.of();
    }

    @Test
    void 커밋되기_전에는_보내지_않고_커밋되면_보낸다() {
        beginTransaction();
        notifier.notifyAfterCommit(1L, "카드 이동");

        verify(slack, after(300).never()).notifyIfConnected(anyLong(), any());

        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        verify(slack, timeout(1000)).notifyIfConnected(1L, "카드 이동");
    }

    @Test
    void 롤백되면_보내지_않는다() {
        beginTransaction();
        notifier.notifyAfterCommit(1L, "카드 이동");

        TransactionSynchronizationManager.getSynchronizations()
                .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(slack, after(500).never()).notifyIfConnected(anyLong(), any());
    }

    @Test
    void 트랜잭션_밖에서는_바로_보낸다() {
        notifier.notifyAfterCommit(1L, "테스트");
        verify(slack, timeout(1000)).notifyIfConnected(1L, "테스트");
    }
}
