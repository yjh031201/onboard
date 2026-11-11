package com.kanban.backend.integration.slack;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Slack 알림을 카드 작업과 떼어서 보낸다.
 *
 * <p>예전에는 카드 이동 같은 작업의 트랜잭션 안에서 Slack에 바로 요청을 보내서, Slack이 느리면 그만큼 카드 이동도
 * 느려지고(보드 잠금을 쥔 채라 같은 프로젝트의 다른 사람도 기다림), Slack이 응답하지 않으면 보드가 멈췄다.
 * 또 저장이 실패해 되돌려진 작업도 Slack에는 이미 알림이 가 있었다.
 *
 * <p>지금은 트랜잭션이 커밋된 뒤에만, 별도 스레드 하나에서 순서대로 보낸다 — 알림 순서는 작업 순서와 같다.
 * 밀린 알림이 너무 많으면(Slack 장애 등) 새 알림은 버리고 로그만 남긴다. 카드 작업은 절대 기다리지 않는다.
 */
@Component
public class SlackNotifier {

    private static final Logger log = LoggerFactory.getLogger(SlackNotifier.class);
    private static final int MAX_PENDING = 200;

    private final SlackIntegrationService slackIntegrationService;
    private final ExecutorService executor = new ThreadPoolExecutor(
            1, 1, 0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(MAX_PENDING),
            runnable -> {
                Thread thread = new Thread(runnable, "slack-notifier");
                thread.setDaemon(true);
                return thread;
            });

    public SlackNotifier(SlackIntegrationService slackIntegrationService) {
        this.slackIntegrationService = slackIntegrationService;
    }

    /** 지금 트랜잭션이 커밋되면 보낸다. 트랜잭션 밖에서 부르면 바로 (비동기로) 보낸다. 롤백되면 보내지 않는다. */
    public void notifyAfterCommit(Long projectId, String text) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    submit(projectId, text);
                }
            });
        } else {
            submit(projectId, text);
        }
    }

    private void submit(Long projectId, String text) {
        try {
            executor.execute(() -> slackIntegrationService.notifyIfConnected(projectId, text));
        } catch (RejectedExecutionException e) {
            log.warn("Slack 알림이 너무 많이 밀려 버렸습니다 (project {}): {}", projectId, text);
        }
    }

    @jakarta.annotation.PreDestroy
    void shutdown() {
        executor.shutdown();
    }
}
