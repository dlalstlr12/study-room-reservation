package com.studyroom.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.studyroom.support.SubscriptionScenarioSupport;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

/** 일일 정기결제 잡 — 결제일 도래한 ACTIVE 구독만 결제하고, 실패 건은 PAST_DUE. */
@SpringBatchTest
class BillingJobTest extends SubscriptionScenarioSupport {

	@Autowired
	JobLauncherTestUtils jobLauncherTestUtils;
	@Autowired
	JobRepositoryTestUtils jobRepositoryTestUtils;

	@AfterEach
	void clearJobs() {
		jobRepositoryTestUtils.removeJobExecutions();
	}

	@Test
	@DisplayName("결제일 도래한 구독 3건 → 배치 실행 → payments 3건, nextBillingAt 한 달 뒤")
	void charges_due_subscriptions() throws Exception {
		List<Subscription> due = List.of(
				dueProSubscription(newMember(), 9900),
				dueProSubscription(newMember(), 9900),
				dueProSubscription(newMember(), 9900));
		// 아직 결제일이 안 된 구독은 건너뛴다
		Subscription notDue = subscriptionRepository.save(Subscription.subscribePro(newMember(), 9900));
		ReflectionTestUtils.setField(notDue, "nextBillingAt", LocalDateTime.now().plusDays(10));
		subscriptionRepository.save(notDue);

		JobExecution execution = jobLauncherTestUtils.launchJob();

		assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
		assertThat(paymentRepository.count()).isEqualTo(3);
		for (Subscription s : due) {
			Subscription reloaded = subscriptionRepository.findById(s.getId()).orElseThrow();
			assertThat(reloaded.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
			assertThat(reloaded.getNextBillingAt()).isAfter(LocalDateTime.now().plusDays(20));
		}
		assertThat(subscriptionRepository.findById(notDue.getId()).orElseThrow().getNextBillingAt())
				.isBefore(LocalDateTime.now().plusDays(11));
	}
}
