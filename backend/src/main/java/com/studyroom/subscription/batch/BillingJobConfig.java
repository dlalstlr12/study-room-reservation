package com.studyroom.subscription.batch;

import com.studyroom.subscription.PaymentService;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.database.JpaCursorItemReader;
import org.springframework.batch.item.database.builder.JpaCursorItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 일일 정기결제 잡. 결제일이 도래한 ACTIVE 구독을 읽어 건별로 결제한다.
 *
 * <ul>
 *   <li>리더는 {@code JpaCursorItemReader} — 한 쿼리 스냅샷이라, 프로세서가 {@code nextBillingAt}
 *       을 옮겨도 페이징 offset이 어긋나 건이 누락되는 문제가 없다.</li>
 *   <li>프로세서가 부르는 {@link PaymentService#chargeForPeriod} 는 {@code REQUIRES_NEW} 라
 *       건별로 독립 커밋 — 한 건 실패가 다른 건을 롤백하지 않는다.</li>
 *   <li>{@code faultTolerant().skip(...)} 로 예외 나도 잡이 죽지 않는다.</li>
 * </ul>
 */
@Configuration
public class BillingJobConfig {

	public static final String JOB_NAME = "dailyBillingJob";

	private static final Logger log = LoggerFactory.getLogger(BillingJobConfig.class);
	private static final int CHUNK = 50;

	@Bean
	public Job dailyBillingJob(JobRepository jobRepository, Step billingStep) {
		return new JobBuilder(JOB_NAME, jobRepository)
				.incrementer(new RunIdIncrementer())
				.start(billingStep)
				.build();
	}

	@Bean
	public Step billingStep(JobRepository jobRepository, PlatformTransactionManager transactionManager,
			JpaCursorItemReader<Long> dueSubscriptionReader,
			ItemProcessor<Long, Long> chargeProcessor) {
		return new StepBuilder("billingStep", jobRepository)
				.<Long, Long>chunk(CHUNK, transactionManager)
				.reader(dueSubscriptionReader)
				.processor(chargeProcessor)
				.writer(chunk -> log.info("[정기결제] 청크 {}건 처리", chunk.size()))
				.faultTolerant()
				.skip(Exception.class)
				.skipLimit(10_000)
				.build();
	}

	@Bean
	@StepScope
	public JpaCursorItemReader<Long> dueSubscriptionReader(EntityManagerFactory entityManagerFactory) {
		return new JpaCursorItemReaderBuilder<Long>()
				.name("dueSubscriptionReader")
				.entityManagerFactory(entityManagerFactory)
				.queryString("select s.id from Subscription s "
						+ "where s.status = com.studyroom.subscription.SubscriptionStatus.ACTIVE "
						+ "and s.nextBillingAt <= :now order by s.id")
				.parameterValues(Map.of("now", LocalDateTime.now()))
				.build();
	}

	@Bean
	@StepScope
	public ItemProcessor<Long, Long> chargeProcessor(PaymentService paymentService) {
		YearMonth period = YearMonth.now();
		return subscriptionId -> {
			paymentService.chargeForPeriod(subscriptionId, period);
			return subscriptionId;
		};
	}
}
