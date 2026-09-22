package com.mylog.analysis.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import com.mylog.analysis.service.CorrectionCommand;

public record CorrectionRequest(
        @Valid @Size(max = 20) List<EmotionCorrection> emotions,
        @Valid @Size(max = 20) List<TopicCorrection> topics) {

    public record EmotionCorrection(
            @NotBlank String type,
            @NotNull Operation operation,
            @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal score) {}

    public record TopicCorrection(
            @NotBlank @Size(max = 100) String name,
            @NotNull Operation operation,
            @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal confidence) {}

    public enum Operation { ADD, UPDATE, REMOVE }

    public CorrectionCommand toCommand() {
        return new CorrectionCommand(
                emotions == null ? List.of() : emotions.stream()
                        .map(item -> new CorrectionCommand.EmotionCorrection(
                                item.type(), CorrectionCommand.Operation.valueOf(item.operation().name()), item.score()))
                        .toList(),
                topics == null ? List.of() : topics.stream()
                        .map(item -> new CorrectionCommand.TopicCorrection(
                                item.name(), CorrectionCommand.Operation.valueOf(item.operation().name()), item.confidence()))
                        .toList());
    }
}
