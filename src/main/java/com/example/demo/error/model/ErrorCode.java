package com.example.demo.error.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum ErrorCode {
    //NotFound 404 error
    FAIL_NOT_ENROLLMENT("해당 수강 신청을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FAIL_NOT_USER("해당 유저를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    FAIL_NOT_COURSE("해당 강의를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

    // BadRequest 400
    ALREADY_ENROLLED("이미 수강 신청한 강의입니다.", HttpStatus.BAD_REQUEST),
    INVALID_COURSE_CLOSE_STATUS("모집 중인 강의만 마감할 수 있습니다.", HttpStatus.BAD_REQUEST),
    INVALID_ENROLLMENT_CANCEL_STATUS("결제 완료된 수강 신청만 취소할 수 있습니다.", HttpStatus.BAD_REQUEST),
    INVALID_CANCEL_PERIOD("결제 후 7일 이내에만 취소할 수 있습니다.", HttpStatus.BAD_REQUEST),
    INVALID_ENROLLMENT_CONFIRM_OWNER("본인의 수강 신청만 결제할 수 있습니다.", HttpStatus.BAD_REQUEST),
    INVALID_SELF_ENROLLMENT("본인이 생성한 강의에는 수강 신청할 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_WAITLIST_PRIORITY("대기자가 존재하여 바로 결제할 수 없습니다. 대기열에 등록되었습니다.", HttpStatus.BAD_REQUEST),
    INVALID_ENROLLMENT_CONFIRM_STATUS("결제 대기 상태의 수강 신청만 확정할 수 있습니다.", HttpStatus.BAD_REQUEST),
    INVALID_ENROLLMENT_OWNER("본인의 수강 신청만 접근할 수 있습니다.", HttpStatus.BAD_REQUEST),
    ALREADY_WAITING("이미 해당 강의의 대기열에 등록되어 있습니다.", HttpStatus.BAD_REQUEST),
    INVALID_ROLE("강의 생성은 강사만 가능합니다.", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE("시작일은 종료일보다 이후일 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_COURSE_OWNER("본인이 생성한 강의만 접근할 수 있습니다.", HttpStatus.BAD_REQUEST),
    INVALID_COURSE_PERIOD("이미 종료된 강의는 오픈할 수 없습니다.", HttpStatus.BAD_REQUEST),
    INVALID_COURSE_CAPACITY("강의 정원이 초과되었습니다.", HttpStatus.BAD_REQUEST),
    INVALID_ENROLLMENT_STATUS("오픈된 강의만 신청 가능합니다.", HttpStatus.BAD_REQUEST),
    INVALID_COURSE_STATUS("현재 강의 상태에서는 오픈할 수 없습니다.", HttpStatus.BAD_REQUEST);

    private String message;
    private HttpStatus statusCode;
}
