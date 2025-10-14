package com.hiresense.resume.domain;

import com.hiresense.resume.domain.enums.ExperienceLevel;
import com.hiresense.resume.dto.request.JobPreferenceUpdateRequest;
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
public class JobPreference {

    private String desiredJob;

    @Enumerated(EnumType.STRING)
    private ExperienceLevel experienceLevel;

    private String description;

    public JobPreference updateFrom(JobPreferenceUpdateRequest request) {
        String updatedDesiredJob = Optional.ofNullable(request.desiredJob()).orElse(this.desiredJob);
        ExperienceLevel updatedExperienceLevel = Optional.ofNullable(request.experienceLevel()).map(s -> EnumConverter.safeValueOf(ExperienceLevel.class, s)).orElse(this.experienceLevel);
        String updatedDescription = Optional.ofNullable(request.description()).orElse(this.description);

        return new JobPreference(updatedDesiredJob, updatedExperienceLevel, updatedDescription);
    }
}
