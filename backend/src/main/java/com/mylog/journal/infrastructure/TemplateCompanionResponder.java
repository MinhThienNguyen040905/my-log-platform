package com.mylog.journal.infrastructure;

import com.mylog.journal.application.CompanionResponder;
import com.mylog.journal.application.command.CompanionTurnCommand;
import com.mylog.journal.application.result.CompanionChip;
import com.mylog.journal.application.result.CompanionReply;
import com.mylog.journal.domain.CompanionMoodTemplates;
import com.mylog.journal.domain.CompanionMoodTemplates.Variant;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Non-generative companion responder: picks one of a fixed set of reviewed Vietnamese replies by
 * mood and draft state. Calls no external provider and never echoes draft or message content back
 * to the user. This is the baseline adapter; an LLM-backed {@code CompanionResponder} is a
 * separate, later adapter gated behind its own approval flag (see the companion plan, stage 4).
 */
@Component
public class TemplateCompanionResponder implements CompanionResponder {
    private static final String PROMPT_VERSION = "companion-template-v1";
    private static final CompanionChip EXIT_CHIP = new CompanionChip("Mình chỉ muốn viết tiếp", "EXIT");
    private static final CompanionChip LONG_TERM_CHIP =
            new CompanionChip("Mình muốn ổn định hơn về lâu dài", "LONG_TERM");

    @Override
    public CompanionReply respond(CompanionTurnCommand command) {
        return switch (command.intent()) {
            case "OPEN" -> fromVariant(command,
                    CompanionMoodTemplates.opening(command.moodCode(), command.draftEmpty(), command.draftLong()),
                    "LISTENING", chipsFor(command.moodCode()));
            case "TOOL_COMPLETED" -> fromVariant(command,
                    CompanionMoodTemplates.toolCompleted(command.moodCode()), "CLOSING", List.of());
            case "CLOSING" -> fromVariant(command, CompanionMoodTemplates.closing(), "CLOSING", List.of());
            default -> fromVariant(command,
                    CompanionMoodTemplates.freeTextFollowUp(command.moodCode()), "LISTENING",
                    List.of(EXIT_CHIP));
        };
    }

    private static List<CompanionChip> chipsFor(String moodCode) {
        List<CompanionChip> chips = new ArrayList<>();
        chips.add(EXIT_CHIP);
        if (CompanionMoodTemplates.offersLongTermChip(moodCode)) chips.add(LONG_TERM_CHIP);
        return List.copyOf(chips);
    }

    private static CompanionReply fromVariant(CompanionTurnCommand command, Variant variant, String nextState,
                                              List<CompanionChip> chips) {
        return new CompanionReply(variant.reply(), variant.questions(), variant.suggestedTitle(), variant.actions(), chips,
                nextState, "TEMPLATE", PROMPT_VERSION);
    }
}
