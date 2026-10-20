package io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands;

import io.fireflysoftware.core.lending.servicing.sdk.model.PaginationResponseLoanRepaymentScheduleDTO;
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
public class GetLoanScheduleQuery implements Query<PaginationResponseLoanRepaymentScheduleDTO> {

    @NotNull
    private UUID loanCaseId;
}
