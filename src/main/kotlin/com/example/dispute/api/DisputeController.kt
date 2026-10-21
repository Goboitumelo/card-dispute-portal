package com.example.dispute.api

import com.example.dispute.security.CustomerPrincipal
import com.example.dispute.service.DisputeService
import com.example.dispute.service.TransactionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.net.URI
import java.util.UUID

@RestController
@RequestMapping("/api/disputes")
class DisputeController(private val disputes: DisputeService) {

    /**
     * Opens a dispute for one of the customer's transactions.
     *
     * @param request the dispute details
     * @return 201 Created with a Location header and the new dispute
     */
    @PostMapping
    fun submit(
        @AuthenticationPrincipal principal: CustomerPrincipal,
        @Valid @RequestBody request: SubmitDisputeRequest,
    ): ResponseEntity<DisputeResponse> {
        val dispute = disputes.submit(principal.customerId, request.toCommand())
        return ResponseEntity
            .created(URI.create("/api/disputes/${dispute.id}"))
            .body(dispute.toResponse())
    }

    /** Lists the customer's disputes, newest first. */
    @GetMapping
    fun list(@AuthenticationPrincipal principal: CustomerPrincipal): List<DisputeResponse> =
        disputes.listFor(principal.customerId).map { it.toResponse() }

    /**
     * Returns one of the customer's disputes.
     *
     * @param id the dispute to return
     * @return the dispute
     */
    @GetMapping("/{id}")
    fun get(
        @AuthenticationPrincipal principal: CustomerPrincipal,
        @PathVariable id: UUID,
    ): DisputeResponse = disputes.getFor(principal.customerId, id).toResponse()

    /**
     * Returns the status history of one of the customer's disputes, oldest first.
     *
     * @param id the dispute whose history to return
     * @return the status changes, oldest first
     */
    @GetMapping("/{id}/history")
    fun history(
        @AuthenticationPrincipal principal: CustomerPrincipal,
        @PathVariable id: UUID,
    ): List<HistoryResponse> =
        disputes.historyFor(principal.customerId, id).map { it.toResponse() }

    /**
     * Withdraws one of the customer's disputes. The request body is optional.
     *
     * @param id the dispute to withdraw
     * @param request an optional note for the withdrawal
     * @return the withdrawn dispute
     */
    @PostMapping("/{id}/withdraw")
    fun withdraw(
        @AuthenticationPrincipal principal: CustomerPrincipal,
        @PathVariable id: UUID,
        @Valid @RequestBody(required = false) request: WithdrawRequest?,
    ): DisputeResponse =
        disputes.withdraw(principal.customerId, id, request?.note).toResponse()
}

@RestController
@RequestMapping("/api/transactions")
class TransactionController(private val transactions: TransactionService) {

    /** Lists the customer's transactions, newest first. */
    @GetMapping
    fun list(@AuthenticationPrincipal principal: CustomerPrincipal): List<TransactionResponse> =
        transactions.listFor(principal.customerId).map { it.toResponse() }
}
