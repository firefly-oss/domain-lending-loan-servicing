/*
 * Copyright 2025 Firefly Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.handlers;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanBalanceApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentRecordApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import io.fireflysoftware.core.lending.servicing.sdk.model.LoanRepaymentScheduleDTO;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.AllocateRepaymentCommand;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RestoreRepaymentAllocationCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@CommandHandlerComponent
public class RestoreRepaymentAllocationHandler extends CommandHandler<RestoreRepaymentAllocationCommand, Void> {

    private final LoanRepaymentRecordApi repaymentRecordApi;
    private final LoanBalanceApi balanceApi;
    private final LoanRepaymentScheduleApi scheduleApi;

    public RestoreRepaymentAllocationHandler(
            LoanRepaymentRecordApi repaymentRecordApi,
            LoanBalanceApi balanceApi,
            LoanRepaymentScheduleApi scheduleApi) {
        this.repaymentRecordApi = repaymentRecordApi;
        this.balanceApi = balanceApi;
        this.scheduleApi = scheduleApi;
    }

    @Override
    protected Mono<Void> doHandle(RestoreRepaymentAllocationCommand cmd) {
        AllocateRepaymentCommand.Allocation allocation = cmd.allocation();
        if (allocation == null) {
            return Mono.empty();
        }
        UUID caseId = allocation.loanServicingCaseId();
        Mono<Void> removeCreatedSchedules = allocation.createdScheduleIds() == null
                ? Mono.empty()
                : Flux.fromIterable(allocation.createdScheduleIds())
                .concatMap(id -> scheduleApi.deleteRepaymentSchedule(caseId, id, key()).onErrorResume(ignored -> Mono.empty()))
                .then();
        Mono<Void> restoreSchedules = allocation.removedSchedules() == null
                ? Mono.empty()
                : Flux.fromIterable(allocation.removedSchedules())
                .concatMap(this::recreateSchedule)
                .then();
        Mono<Void> restoreBalance = allocation.previousBalanceId() == null
                ? Mono.empty()
                : balanceApi.getBalanceById(caseId, allocation.previousBalanceId(), key())
                .flatMap(previous -> {
                    previous.setIsCurrent(true);
                    return balanceApi.updateBalance(caseId, previous.getLoanBalanceId(), previous, key());
                })
                .then();
        Mono<Void> deleteNewBalance = allocation.newBalanceId() == null
                ? Mono.empty()
                : balanceApi.deleteBalance(caseId, allocation.newBalanceId(), key()).onErrorResume(ignored -> Mono.empty());
        Mono<Void> deleteRepayment = allocation.repaymentId() == null
                ? Mono.empty()
                : repaymentRecordApi.deleteRepaymentRecord(caseId, allocation.repaymentId(), key())
                .onErrorResume(ignored -> Mono.empty());
        return removeCreatedSchedules
                .then(restoreSchedules)
                .then(restoreBalance)
                .then(deleteNewBalance)
                .then(deleteRepayment);
    }

    private Mono<LoanRepaymentScheduleDTO> recreateSchedule(LoanRepaymentScheduleDTO source) {
        LoanRepaymentScheduleDTO copy = new LoanRepaymentScheduleDTO();
        copy.setLoanServicingCaseId(source.getLoanServicingCaseId());
        copy.setInstallmentNumber(source.getInstallmentNumber());
        copy.setDueDate(source.getDueDate());
        copy.setPrincipalDue(source.getPrincipalDue());
        copy.setInterestDue(source.getInterestDue());
        copy.setFeeDue(source.getFeeDue());
        copy.setTotalDue(source.getTotalDue());
        copy.setIsPaid(source.getIsPaid());
        copy.setPaidDate(source.getPaidDate());
        copy.setPaidAmount(source.getPaidAmount());
        return scheduleApi.createRepaymentSchedule(source.getLoanServicingCaseId(), copy, key());
    }

    private static String key() {
        return UUID.randomUUID().toString();
    }
}
