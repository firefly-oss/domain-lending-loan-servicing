package io.fireflysoftware.domain.lending.loan.servicing.infra;

import io.fireflysoftware.core.lending.servicing.sdk.api.LoanAccrualApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanBalanceApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanDisbursementApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanDisbursementPlanApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanEscrowApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanInstallmentPlanApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanInstallmentRecordApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanNotificationApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRateChangeApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRebateApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentRecordApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanRestructuringApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanServicingCaseApi;
import io.fireflysoftware.core.lending.servicing.sdk.api.LoanServicingEventApi;
import io.fireflysoftware.core.lending.servicing.sdk.invoker.ApiClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * Default implementation of the ClientFactory interface.
 * Creates client service instances using the appropriate API clients and dependencies.
 */
@Component
public class ClientFactory {

    private final ApiClient apiClient;

    @Autowired
    public ClientFactory(
            LoanServicingProperties loanServicingProperties) {
        this.apiClient = new ApiClient();
        this.apiClient.setBasePath(loanServicingProperties.getBasePath());
    }

    @Bean
    public LoanServicingCaseApi loanServicingCaseApi(){
        return new LoanServicingCaseApi(apiClient);
    }

    @Bean
    public LoanAccrualApi loanAccrualApi(){
        return new LoanAccrualApi(apiClient);
    }

    @Bean
    public LoanDisbursementApi loanDisbursementApi(){
        return new LoanDisbursementApi(apiClient);
    }

    @Bean
    public LoanRateChangeApi loanRateChangeApi(){
        return new LoanRateChangeApi(apiClient);
    }

    @Bean
    public LoanRepaymentRecordApi loanRepaymentRecordApi(){
        return new LoanRepaymentRecordApi(apiClient);
    }

    @Bean
    public LoanRepaymentScheduleApi loanRepaymentScheduleApi(){
        return new LoanRepaymentScheduleApi(apiClient);
    }

    @Bean
    public LoanServicingEventApi loanServicingEventApi(){
        return new LoanServicingEventApi(apiClient);
    }

    @Bean
    public LoanBalanceApi loanBalanceApi(){
        return new LoanBalanceApi(apiClient);
    }

    @Bean
    public LoanInstallmentPlanApi loanInstallmentPlanApi(){
        return new LoanInstallmentPlanApi(apiClient);
    }

    @Bean
    public LoanInstallmentRecordApi loanInstallmentRecordApi(){
        return new LoanInstallmentRecordApi(apiClient);
    }

    @Bean
    public LoanDisbursementPlanApi loanDisbursementPlanApi(){
        return new LoanDisbursementPlanApi(apiClient);
    }

    @Bean
    public LoanEscrowApi loanEscrowApi(){
        return new LoanEscrowApi(apiClient);
    }

    @Bean
    public LoanRebateApi loanRebateApi(){
        return new LoanRebateApi(apiClient);
    }

    @Bean
    public LoanRestructuringApi loanRestructuringApi(){
        return new LoanRestructuringApi(apiClient);
    }

    @Bean
    public LoanNotificationApi loanNotificationApi(){
        return new LoanNotificationApi(apiClient);
    }

}
