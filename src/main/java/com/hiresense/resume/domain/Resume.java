package com.hiresense.resume.domain;

import com.hiresense.resume.domain.enums.AcademicStatus;
import com.hiresense.resume.domain.enums.EmploymentType;
import com.hiresense.resume.domain.enums.ExperienceLevel;
import com.hiresense.resume.domain.enums.Gender;
import com.hiresense.resume.dto.request.ResumeRequest;
import com.hiresense.resume.dto.request.ResumeUpdateRequest;
import com.hiresense.global.entity.BaseTimeEntity;
import com.hiresense.global.util.EnumConverter;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Resume extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String address;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private String birthYear;
    private String phone;
    private String email;
    private String homePhone;

    @Embedded
    private AcademicRecord academicRecord;

    @Embedded
    private JobPreference jobPreference;

    private String desiredRegion;
    private Integer desiredSalary;

    @Embedded
    private WorkCondition workCondition;

    @Builder
    public Resume(String name, String address, Gender gender, String birthYear, String phone, String email, String homePhone, AcademicRecord academicRecord, JobPreference jobPreference, String desiredRegion, Integer desiredSalary, WorkCondition workCondition) {
        this.name = name;
        this.address = address;
        this.gender = gender;
        this.birthYear = birthYear;
        this.phone = phone;
        this.email = email;
        this.homePhone = homePhone;
        this.academicRecord = academicRecord;
        this.jobPreference = jobPreference;
        this.desiredRegion = desiredRegion;
        this.desiredSalary = desiredSalary;
        this.workCondition = workCondition;
    }

    public static Resume createFrom(ResumeRequest request) {
        return Resume.builder()
            .name(request.name())
            .address(request.address())
            .gender(EnumConverter.safeValueOf(Gender.class, request.gender()))
            .birthYear(request.birthYear())
            .phone(request.phone())
            .email(request.email())
            .homePhone(request.homePhone())
            .academicRecord(new AcademicRecord(
                request.academicRecord().schoolName(),
                request.academicRecord().period(),
                EnumConverter.safeValueOf(AcademicStatus.class, request.academicRecord().status()),
                request.academicRecord().gpa(),
                request.academicRecord().major()
            ))
            .jobPreference(new JobPreference(
                request.jobPreference().desiredJob(),
                EnumConverter.safeValueOf(ExperienceLevel.class, request.jobPreference().experienceLevel()),
                request.jobPreference().description()
            ))
            .desiredRegion(request.desiredRegion())
            .desiredSalary(request.desiredSalary())
            .workCondition(new WorkCondition(
                EnumConverter.safeValueOf(EmploymentType.class, request.workCondition().employmentType()),
                request.workCondition().desiredHours()
            ))
            .build();
    }

    public void updateFrom(ResumeUpdateRequest request) {
        Optional.ofNullable(request.name()).ifPresent(value -> this.name = value);
        Optional.ofNullable(request.address()).ifPresent(value -> this.address = value);
        Optional.ofNullable(request.gender()).ifPresent(value -> this.gender = EnumConverter.safeValueOf(Gender.class, value));
        Optional.ofNullable(request.birthYear()).ifPresent(value -> this.birthYear = value);
        Optional.ofNullable(request.phone()).ifPresent(value -> this.phone = value);
        Optional.ofNullable(request.email()).ifPresent(value -> this.email = value);
        Optional.ofNullable(request.homePhone()).ifPresent(value -> this.homePhone = value);
        Optional.ofNullable(request.desiredRegion()).ifPresent(value -> this.desiredRegion = value);
        Optional.ofNullable(request.desiredSalary()).ifPresent(value -> this.desiredSalary = value);

        if (request.academicRecord() != null) {
            this.academicRecord = academicRecord.updateFrom(request.academicRecord());
        }
        if (request.jobPreference() != null) {
            this.jobPreference = jobPreference.updateFrom(request.jobPreference());
        }
        if (request.workCondition() != null) {
            this.workCondition = workCondition.updateFrom(request.workCondition());
        }
    }
}
