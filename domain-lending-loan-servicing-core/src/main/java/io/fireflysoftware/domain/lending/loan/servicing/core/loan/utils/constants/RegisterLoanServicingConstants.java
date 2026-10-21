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


package io.fireflysoftware.domain.lending.loan.servicing.core.loan.utils.constants;

public class RegisterLoanServicingConstants {

    // ============================== SAGA CONFIGURATION ==============================
    public static final String SAGA_REGISTER_LOAN_SERVICING = "RegisterLoanServicingSaga";

    // ============================== STEP IDENTIFIERS ==============================
    public static final String STEP_REGISTER_LOAN_SERVICING = "registerLoanServicing";
    public static final String STEP_REGISTER_LOAN_ACCRUAL = "registerLoanAccrual";
    public static final String STEP_REGISTER_LOAN_DISBURSEMENT = "registerLoanDisbursement";
    public static final String STEP_REGISTER_LOAN_RATE_CHANGE = "registerLoanRateChange";
    public static final String STEP_REGISTER_LOAN_REPAYMENT_RECORD = "registerLoanRepaymentRecord";
    public static final String STEP_REGISTER_LOAN_REPAYMENT_SCHEDULE = "registerLoanRepaymentSchedule";
    public static final String STEP_REGISTER_LOAN_SERVICING_EVENT = "registerLoanServicingEvent";

    // ============================== COMPENSATE METHODS ==============================
    public static final String COMPENSATE_REMOVE_LOAN_SERVICING = "removeLoanServicing";
    public static final String COMPENSATE_REMOVE_LOAN_ACCRUAL = "removeLoanAccrual";
    public static final String COMPENSATE_REMOVE_LOAN_DISBURSEMENT = "removeLoanDisbursement";
    public static final String COMPENSATE_REMOVE_LOAN_RATE_CHANGE = "removeLoanRateChange";
    public static final String COMPENSATE_REMOVE_LOAN_REPAYMENT_RECORD = "removeLoanRepaymentRecord";
    public static final String COMPENSATE_REMOVE_LOAN_REPAYMENT_SCHEDULE = "removeLoanRepaymentSchedule";
    public static final String COMPENSATE_REMOVE_LOAN_SERVICING_EVENT = "removeLoanServicingEvent";

    // ============================== EVENT TYPES ==============================
    public static final String EVENT_LOAN_SERVICING_REGISTERED = "loanServicing.registered";
    public static final String EVENT_LOAN_ACCRUAL_REGISTERED = "loanAccrual.registered";
    public static final String EVENT_LOAN_DISBURSEMENT_REGISTERED = "loanDisbursement.registered";
    public static final String EVENT_LOAN_RATE_CHANGE_REGISTERED = "loanRateChange.registered";
    public static final String EVENT_LOAN_REPAYMENT_RECORD_REGISTERED = "loanRepaymentRecord.registered";
    public static final String EVENT_LOAN_REPAYMENT_SCHEDULE_REGISTERED = "loanRepaymentSchedule.registered";
    public static final String EVENT_LOAN_SERVICING_EVENT_REGISTERED = "loanServicingEvent.registered";


}
