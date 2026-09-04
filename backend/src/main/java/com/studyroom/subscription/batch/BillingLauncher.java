package com.studyroom.subscription.batch;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 정기결제 잡 실행 진입점. 매일 자정 스케줄러가, 그리고 ADMIN이 수동으로 호출한다.
 * 실행마다 고유 파라미터({@code requestedAt})를 넣어 새 JobInstance 로 돌린다.
 */
@Component
public class BillingLauncher {

	private static final Logger log = LoggerFactory.getLogger(BillingLauncher.class);

	private final JobLauncher jobLauncher;
	private final Job dailyBillingJob;

	public BillingLauncher(JobLauncher jobLauncher, Job dailyBillingJob) {
		this.jobLauncher = jobLauncher;
		this.dailyBillingJob = dailyBillingJob;
	}

	public JobExecution run() throws Exception {
		return jobLauncher.run(dailyBillingJob, new JobParametersBuilder()
				.addLocalDateTime("requestedAt", LocalDateTime.now())
				.toJobParameters());
	}

	@Scheduled(cron = "${subscription.billing.cron}")
	public void runDaily() {
		try {
			JobExecution execution = run();
			log.info("[정기결제] 스케줄 실행 완료 status={}", execution.getStatus());
		} catch (Exception e) {
			log.error("[정기결제] 스케줄 실행 실패", e);
		}
	}
}
