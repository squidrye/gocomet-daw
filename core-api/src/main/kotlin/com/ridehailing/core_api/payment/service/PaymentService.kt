package com.ridehailing.core_api.payment

import com.ridehailing.core_api.common.exception.AppException
import com.ridehailing.core_api.common.exception.AppExceptionTypes
import com.ridehailing.core_api.common.model.Payment
import com.ridehailing.core_api.common.model.PaymentStatus
import com.ridehailing.core_api.common.model.RideStatus
import com.ridehailing.core_api.common.util.IdempotencyHash
import com.ridehailing.core_api.payment.dto.PaymentRequest
import com.ridehailing.core_api.payment.dto.PaymentResponse
import com.ridehailing.core_api.ride.RideMapper
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
open class PaymentService {

  private val log = LoggerFactory.getLogger(this::class.java)

  @Autowired
  private lateinit var paymentMapper: PaymentMapper

  @Autowired
  private lateinit var rideMapper: RideMapper

  @Autowired
  private lateinit var pspStub: PspStub

  @Transactional
  fun processPayment(riderId: UUID, request: PaymentRequest): PaymentResponse {
    log.info("processPayment - riderId=$riderId, rideId=${request.rideId}")

    if (request.rideId == null) throw AppException(AppExceptionTypes.VALIDATION_FAILED, listOf("rideId is required"))

    val ride = rideMapper.getByIdForUpdate(request.rideId!!) ?: throw AppException(AppExceptionTypes.RIDE_NOT_FOUND)
    if (ride.riderId != riderId) throw AppException(AppExceptionTypes.PAYMENT_ACCESS_DENIED)
    if (ride.status != RideStatus.COMPLETED) throw AppException(AppExceptionTypes.PAYMENT_RIDE_NOT_COMPLETED)

    val existingPayment = paymentMapper.getByRideId(ride.id!!)
    if (existingPayment != null) {
      log.info("processPayment - payment already exists for ride ${ride.id}")
      return PaymentResponse().apply {
        id = existingPayment.id
        rideId = existingPayment.rideId
        amount = existingPayment.amount
        transactionId = existingPayment.transactionId
        status = existingPayment.status?.name
      }
    }

    val amount = ride.finalFare ?: throw AppException(AppExceptionTypes.RIDE_NO_FINAL_FARE)
    val transactionId = pspStub.processPayment(amount)

    val payment = Payment().apply {
      this.rideId = ride.id
      this.amount = amount
      this.transactionId = transactionId
      setStatus(PaymentStatus.SUCCESS)
      this.hash = IdempotencyHash.generate(ride.id)
    }
    paymentMapper.insert(payment)
    log.info("processPayment - payment recorded id=${payment.id}, txn=$transactionId")

    return PaymentResponse().apply {
      id = payment.id
      rideId = payment.rideId
      this.amount = payment.amount
      this.transactionId = payment.transactionId
      status = payment.status?.name
    }
  }
}
