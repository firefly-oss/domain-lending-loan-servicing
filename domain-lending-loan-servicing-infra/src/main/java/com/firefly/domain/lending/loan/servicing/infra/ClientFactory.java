package com.firefly.domain.lending.loan.servicing.infra;

import com.firefly.core.lending.servicing.sdk.api.LoanAccrualApi;
import com.firefly.core.lending.servicing.sdk.api.LoanDisbursementApi;
import com.firefly.core.lending.servicing.sdk.api.LoanRateChangeApi;
import com.firefly.core.lending.servicing.sdk.api.LoanRepaymentRecordApi;
import com.firefly.core.lending.servicing.sdk.api.LoanRepaymentScheduleApi;
import com.firefly.core.lending.servicing.sdk.api.LoanServicingCaseApi;
import com.firefly.core.lending.servicing.sdk.api.LoanServicingEventApi;
import com.firefly.core.lending.servicing.sdk.invoker.ApiClient;
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

}
