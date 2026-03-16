package com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands;

import com.firefly.core.lending.servicing.sdk.model.PaginationResponseLoanDisbursementDTO;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.fireflyframework.cqrs.query.Query;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetLoanDisbursementsQuery implements Query<PaginationResponseLoanDisbursementDTO> {

    @NotNull
    private UUID loanCaseId;
}
