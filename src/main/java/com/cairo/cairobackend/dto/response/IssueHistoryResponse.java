package com.cairo.cairobackend.dto.response;

import com.cairo.cairobackend.entity.IssueHistory;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@Builder
public class IssueHistoryResponse {
    private Long          id;
    private String        fieldName;
    private String        oldValue;
    private String        newValue;
    private String        changedBy;
    private LocalDateTime changedAt;

    public static IssueHistoryResponse from(IssueHistory h) {
        return IssueHistoryResponse.builder()
                .id(h.getId())
                .fieldName(h.getFieldName())
                .oldValue(h.getOldValue())
                .newValue(h.getNewValue())
                .changedBy(h.getChangedBy().getName())
                .changedAt(h.getChangedAt())
                .build();
    }
}