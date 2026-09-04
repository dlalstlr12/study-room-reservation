package com.studyroom.subscription;

import com.studyroom.common.security.MemberPrincipal;
import com.studyroom.subscription.dto.PaymentResponse;
import com.studyroom.subscription.dto.SubscriptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Subscription", description = "정기 구독 (PRO 홀딩 연장)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

	private final SubscriptionService subscriptionService;

	public SubscriptionController(SubscriptionService subscriptionService) {
		this.subscriptionService = subscriptionService;
	}

	@Operation(summary = "내 구독", description = "구독한 적 없으면 FREE.")
	@GetMapping("/me")
	public SubscriptionResponse me(@AuthenticationPrincipal MemberPrincipal principal) {
		return subscriptionService.mySubscription(principal.memberId());
	}

	@Operation(summary = "PRO 구독", description = "다음 정기결제 배치가 첫 결제를 처리한다.")
	@PostMapping
	public SubscriptionResponse subscribe(@AuthenticationPrincipal MemberPrincipal principal) {
		return subscriptionService.subscribePro(principal.memberId());
	}

	@Operation(summary = "구독 해지")
	@PostMapping("/cancel")
	public SubscriptionResponse cancel(@AuthenticationPrincipal MemberPrincipal principal) {
		return subscriptionService.cancel(principal.memberId());
	}

	@Operation(summary = "내 결제 이력", description = "최근 20건.")
	@GetMapping("/me/payments")
	public List<PaymentResponse> payments(@AuthenticationPrincipal MemberPrincipal principal) {
		return subscriptionService.myPayments(principal.memberId());
	}
}
