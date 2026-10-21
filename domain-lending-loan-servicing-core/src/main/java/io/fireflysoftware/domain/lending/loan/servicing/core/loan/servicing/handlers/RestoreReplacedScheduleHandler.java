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

import io.fireflysoftware.domain.lending.loan.servicing.core.loan.finance.LoanCalendarPort;
import io.fireflysoftware.domain.lending.loan.servicing.core.loan.servicing.commands.RestoreReplacedScheduleCommand;
import org.fireflyframework.cqrs.annotations.CommandHandlerComponent;
import org.fireflyframework.cqrs.command.CommandHandler;
import reactor.core.publisher.Mono;

@CommandHandlerComponent
public class RestoreReplacedScheduleHandler extends CommandHandler<RestoreReplacedScheduleCommand, Void> {

    private final LoanCalendarPort calendars;

    public RestoreReplacedScheduleHandler(LoanCalendarPort calendars) {
        this.calendars = calendars;
    }

    @Override
    protected Mono<Void> doHandle(RestoreReplacedScheduleCommand cmd) {
        if (cmd.revision() == null) {
            return Mono.empty();
        }
        return calendars.restore(cmd.revision()).then();
    }
}
