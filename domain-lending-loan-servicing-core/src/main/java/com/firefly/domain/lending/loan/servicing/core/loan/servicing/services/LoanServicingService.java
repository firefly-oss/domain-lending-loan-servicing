package com.firefly.domain.lending.loan.servicing.core.loan.servicing.services;

import org.fireflyframework.transactional.saga.core.SagaResult;

import com.firefly.domain.lending.loan.servicing.core.loan.servicing.commands.*;
import reactor.core.publisher.Mono;


public interface LoanServicingService {

    /**
     * Submits a loan case by processing the provided command details.
     *
     * @param command the command containing the necessary information to submit the loan case
     * @return a {@code Mono<SagaResult>} representing the result of the loan case submission process
     */
    Mono<SagaResult> submitLoanCase(SubmitLoanCaseCommand command);

    /**
     * Handles the disbursement of a loan based on the provided loan identifier and command details.
     *
     * @param loanId the identifier of the loan to disburse
     * @param command the command containing details necessary for the disbursement process
     * @return a Mono that completes when the disbursement action is processed
     */
    Mono<SagaResult> disburse(String loanId, DisburseCommand command);

    /**
     * Generates a repayment schedule for the specified loan.
     *
     * @param loanId the unique identifier of the loan for which the repayment schedule is to be generated
     * @param command an instance of GenerateScheduleCommand containing the details required to generate the schedule
     * @return a Mono that completes when the schedule generation process is successfully executed
     */
    Mono<SagaResult> generateSchedule(String loanId, GenerateScheduleCommand command);

    /**
     * Applies a repayment to the loan specified by the given loan ID using the details provided
     * in the ApplyRepaymentCommand.
     *
     * @param loanId the unique identifier of the loan to which the repayment will be applied
     * @param command the command containing the details of the repayment to be applied
     * @return a Mono that completes when the repayment is successfully applied
     */
    Mono<SagaResult> applyRepayment(String loanId, ApplyRepaymentCommand command);

    /**
     * Applies a prepayment to the specified loan account using the provided prepayment command.
     *
     * @param loanId the unique identifier of the loan to which the prepayment will be applied
     * @param command the command object containing the details of the prepayment to be applied
     * @return a Mono that completes when the operation is successfully executed
     */
    Mono<SagaResult> applyPrepayment(String loanId, ApplyPrepaymentCommand command);

    /**
     * Accrues interest on a loan based on the provided command.
     *
     * @param loanId the unique identifier of the loan for which interest is to be accrued
     * @param command the command containing the necessary details and parameters for accruing interest
     * @return a {@code Mono<SagaResult>} that completes when the interest accrual operation is successful
     */
    Mono<SagaResult> accrueInterest(String loanId, AccrueInterestCommand command);

    /**
     * Capitalizes accrued interest for the specified loan.
     *
     * @param loanId the unique identifier of the loan for which interest will be capitalized
     * @param command the command containing the details required to perform the capitalization
     * @return a Mono signaling when the capitalization process has completed
     */
    Mono<SagaResult> capitalizeInterest(String loanId, CapitalizeInterestCommand command);

    /**
     * Triggers the generation of penalties for a specific loan identified by the given loan ID 
     * based on the provided command information.
     *
     * @param loanId the unique identifier of the loan for which penalties are to be generated
     * @param command the command containing the necessary details and context to generate penalties
     * @return a Mono that completes when the penalty generation process is finished
     */
    Mono<SagaResult> generatePenalties(String loanId, GeneratePenaltiesCommand command);

    /**
     * Sets a fixed interest rate for the specified loan.
     *
     * @param loanId the unique identifier of the loan for which the fixed rate is being set
     * @param command the command containing the details required to set the fixed rate
     * @return a {@code Mono<SagaResult>} that completes when the operation is finished
     */
    Mono<SagaResult> setFixedRate(String loanId, SetFixedRateCommand command);

    /**
     * Updates the reference index for a given loan.
     *
     * @param loanId the unique identifier of the loan whose reference index needs to be changed
     * @param command the command object containing the details necessary for changing the reference index
     * @return a Mono that completes when the reference index has been successfully changed
     */
    Mono<SagaResult> changeReferenceIndex(String loanId, ChangeReferenceIndexCommand command);

    /**
     * Grants a payment holiday for a specific loan identified by its loan ID.
     *
     * @param loanId the unique identifier of the loan for which the payment holiday is being granted
     * @param command the command containing the details required for granting the payment holiday
     * @return a Mono signaling completion when the payment holiday is successfully granted
     */
    Mono<SagaResult> grantPaymentHoliday(String loanId, GrantPaymentHolidayCommand command);

    /**
     * Ends a payment holiday for the specified loan.
     *
     * @param loanId the unique identifier of the loan for which the payment holiday is to be ended
     * @param command the command containing the necessary information to end the payment holiday
     * @return a Mono that completes when the operation is finished
     */
    Mono<SagaResult> endPaymentHoliday(String loanId, EndPaymentHolidayCommand command);

    /**
     * Applies a restructure operation to the specified loan.
     *
     * @param loanId the identifier of the loan to which the restructure operation will be applied
     * @param command the command containing the details of the restructure operation
     * @return a Mono that completes when the restructure operation has been successfully applied
     */
    Mono<SagaResult> applyRestructure(String loanId, ApplyRestructureCommand command);

    /**
     * Marks a loan as being in arrears based on the provided command. This operation adjusts the status
     * of the loan to reflect that it is behind on its scheduled payments.
     *
     * @param loanId the unique identifier of the loan that needs to be marked in arrears
     * @param command the command object containing the information required to mark the loan in arrears
     * @return a Mono that completes when the operation is successful
     */
    Mono<SagaResult> markInArrears(String loanId, MarkInArrearsCommand command);

    /**
     * Normalizes the arrears state of a loan identified by the given loan ID.
     *
     * @param loanId the unique identifier of the loan whose arrears need to be normalized
     * @param command the command object containing the details and parameters for the normalization operation
     * @return a Mono signaling completion when the arrears normalization process is completed
     */
    Mono<SagaResult> normalizeArrears(String loanId, NormalizeArrearsCommand command);

    /**
     * Marks a loan as charged off, indicating that the loan is unlikely to be collected 
     * and is classified as a loss in financial records.
     *
     * @param loanId the unique identifier of the loan to be charged off
     * @param command the command containing details needed to perform the charge-off operation
     * @return a {@code Mono<SagaResult>} representing the completion of the charge-off operation
     */
    Mono<SagaResult> chargeOff(String loanId, ChargeOffCommand command);

    /**
     * Performs a write-back operation on a loan, updating its state based on the specified command.
     *
     * @param loanId the unique identifier of the loan to be updated
     * @param command the command containing the details to apply for the write-back operation
     * @return a Mono signaling when the write-back operation has been completed
     */
    Mono<SagaResult> writeBack(String loanId, WriteBackCommand command);

    /**
     * Closes a loan associated with the specified loan ID by executing the provided CloseLoanCommand.
     *
     * @param loanId the unique identifier of the loan to be closed
     * @param command the command containing the necessary information for closing the loan
     * @return a {@code Mono<SagaResult>} that completes when the loan closure process is finished
     */
    Mono<SagaResult> closeLoan(String loanId, CloseLoanCommand command);

    /**
     * Retrieves loan details for a specific loan.
     *
     * @param loanId the unique identifier of the loan for which details are to be retrieved
     * @return a {@code Mono<Object>} providing the loan details asynchronously
     */
    Mono<Object> getLoanDetails(String loanId);

}
