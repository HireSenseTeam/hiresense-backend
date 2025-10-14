package com.hiresense.resume.domain;

import com.hiresense.resume.domain.enums.AcademicStatus;
import com.hiresense.resume.dto.request.AcademicRecordUpdateRequest;
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
public class AcademicRecord {

    private String schoolName;
    private String period;

    @Enumerated(EnumType.STRING)
    private AcademicStatus status;

    private Double gpa;
    private String major;

    public AcademicRecord updateFrom(AcademicRecordUpdateRequest request) {
        String updatedSchoolName = Optional.ofNullable(request.schoolName()).orElse(this.schoolName);
        String updatedPeriod = Optional.ofNullable(request.period()).orElse(this.period);
        AcademicStatus updatedStatus = Optional.ofNullable(request.status()).map(s -> EnumConverter.safeValueOf(AcademicStatus.class, s)).orElse(this.status);
        Double updatedGpa = Optional.ofNullable(request.gpa()).orElse(this.gpa);
        String updatedMajor = Optional.ofNullable(request.major()).orElse(this.major);

        return new AcademicRecord(updatedSchoolName, updatedPeriod, updatedStatus, updatedGpa, updatedMajor);
    }
}
