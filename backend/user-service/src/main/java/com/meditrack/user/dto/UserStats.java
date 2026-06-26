package com.meditrack.user.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStats {
    private long adminCount;
    private long doctorCount;
    private long nurseCount;
    private long activeCount;
    private long totalCount;
}
