package com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands;

import com.firefly.core.lending.servicing.sdk.model.PaginationResponse;
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
public class GetLoanNotificationsQuery implements Query<PaginationResponse> {

    @NotNull
    private UUID loanCaseId;
}
