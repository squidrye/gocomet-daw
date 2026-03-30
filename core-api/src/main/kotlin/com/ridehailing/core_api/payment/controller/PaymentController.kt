package com.ridehailing.core_api.payment

import com.ridehailing.core_api.payment.dto.PaymentRequest
import com.ridehailing.core_api.payment.dto.PaymentResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments")
open class PaymentController {

  @Autowired
  private lateinit var paymentService: PaymentService

  @Operation(summary = "Process payment for a completed ride")
  @PostMapping
  fun processPayment(@RequestBody request: PaymentRequest): ResponseEntity<PaymentResponse> {
    val riderId = getAuthUserId()
    val response = paymentService.processPayment(riderId, request)
    return ResponseEntity.ok(response)
  }

  private fun getAuthUserId(): UUID {
    return SecurityContextHolder.getContext().authentication.principal as UUID
  }
}
