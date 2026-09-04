package com.studyroom.subscription;

import com.studyroom.subscription.batch.BillingLauncher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.batch.core.JobExecution;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Billing", description = "정기결제 배치 (ADMIN)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/admin/billing")
public class BillingController {

	private final BillingLauncher billingLauncher;

	public BillingController(BillingLauncher billingLauncher) {
		this.billingLauncher = billingLauncher;
	}

	@Operation(summary = "정기결제 배치 즉시 실행", description = "ADMIN. 결제일이 도래한 구독을 지금 결제한다.")
	@PostMapping("/run")
	public Map<String, Object> run() throws Exception {
		JobExecution execution = billingLauncher.run();
		return Map.of(
				"jobExecutionId", execution.getId(),
				"status", execution.getStatus().toString());
	}
}
