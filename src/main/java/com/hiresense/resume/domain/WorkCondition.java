package com.hiresense.resume.domain;

import com.hiresense.resume.domain.enums.EmploymentType;
import com.hiresense.resume.dto.request.WorkConditionUpdateRequest;
import com.hiresense.global.util.EnumConverter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class WorkCondition {

    @Enumerated(EnumType.STRING)
    private EmploymentType employmentType;

    private String desiredHours;

    public WorkCondition updateFrom(WorkConditionUpdateRequest request) {
        EmploymentType updatedEmploymentType = Optional.ofNullable(request.employmentType()).map(s -> EnumConverter.safeValueOf(EmploymentType.class, s)).orElse(this.employmentType);
        String updatedDesiredHours = Optional.ofNullable(request.desiredHours()).orElse(this.desiredHours);

        return new WorkCondition(updatedEmploymentType, updatedDesiredHours);
    }
}
