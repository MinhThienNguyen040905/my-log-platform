package com.mylog.journal.application;

import com.mylog.journal.application.command.CompanionTurnCommand;
import com.mylog.journal.application.result.CompanionReply;

/**
 * Produces the companion's reply for a turn that has already passed the feature flag, quota,
 * consent and safety gates in {@code WritingSuggestionService}. Implementations never see a turn
 * that safety has flagged; see ADR-0004 for the port/adapter split this follows (template today,
 * an LLM adapter once approved).
 */
public interface CompanionResponder {
    CompanionReply respond(CompanionTurnCommand command);
}
