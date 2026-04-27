package com.example.demo.api.integration.enrollment;

import com.example.demo.api.dto.enrollment.EnrollmentResponseDto;
import com.example.demo.api.dto.enrollment.EnrollmentResult;
import com.example.demo.api.persistence.entity.*;
import com.example.demo.api.persistence.repository.CourseRepository;
import com.example.demo.api.persistence.repository.EnrollmentRepository;
import com.example.demo.api.persistence.repository.UserRepository;
import com.example.demo.api.persistence.repository.WaitlistRepository;
import com.example.demo.api.service.EnrollmentCommandService;;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class EnrollmentConcurrencyTest {
    @Autowired
    private EnrollmentCommandService enrollmentCommandService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private EntityManager entityManager;

    @AfterEach
    void tearDown() {
        waitlistRepository.deleteAllInBatch();
        enrollmentRepository.deleteAllInBatch();
        courseRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("수강 확정 동시성 테스트 - 정원이 1개 남았을 때 동시에 요청해도 1명만 확정된다")
    void confirm_concurrency_only_one_success_when_one_capacity_left() throws Exception {
        // Given
        int threadCount = 100;

        User creator = userRepository.save(User.create("강사", Role.CREATOR));

        Course course = createOpenCourse(creator, 10, 9);
        Course savedCourse = courseRepository.save(course);

        List<Enrollment> enrollments = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            User student = userRepository.save(User.create("학생" + i, Role.STUDENT));
            Enrollment enrollment = enrollmentRepository.save(
                    Enrollment.create(student, savedCourse)
            );
            enrollments.add(enrollment);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<EnrollmentResponseDto>> futures = new ArrayList<>();

        for (Enrollment enrollment : enrollments) {
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                return enrollmentCommandService.confirm(
                        enrollment.getUser().getId(),
                        enrollment.getId()
                );
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        List<EnrollmentResponseDto> responses = new ArrayList<>();

        for (Future<EnrollmentResponseDto> future : futures) {
            responses.add(future.get());
        }

        executorService.shutdown();

        entityManager.clear();

        // Then
        Course resultCourse = courseRepository.findById(savedCourse.getId()).orElseThrow();

        List<Enrollment> confirmedEnrollments =
                enrollmentRepository.findAllByCourseAndEnrollmentStatus(
                        resultCourse,
                        EnrollmentStatus.CONFIRMED
                );

        List<Enrollment> cancelledEnrollments =
                enrollmentRepository.findAllByCourseAndEnrollmentStatus(
                        resultCourse,
                        EnrollmentStatus.CANCELLED
                );

        long confirmedResponseCount = responses.stream()
                .filter(response -> response.type() == EnrollmentResult.CONFIRMED)
                .count();

        long waitlistResponseCount = responses.stream()
                .filter(response -> response.type() == EnrollmentResult.WAITLIST)
                .count();

        assertThat(resultCourse.getCurrentCapacity()).isEqualTo(10);
        assertThat(confirmedEnrollments).hasSize(1);
        assertThat(cancelledEnrollments).hasSize(threadCount - 1);

        assertThat(confirmedResponseCount).isEqualTo(1);
        assertThat(waitlistResponseCount).isEqualTo(threadCount - 1);
    }

    @Test
    @DisplayName("수강 확정 동시성 테스트 - 같은 사용자가 동일 수강 신청을 동시에 확정해도 한 번만 확정된다")
    void confirm_concurrency_same_user_same_enrollment_only_once_success() throws Exception {
        // Given
        int threadCount = 100;

        User creator = userRepository.save(User.create("강사", Role.CREATOR));
        User student = userRepository.save(User.create("학생", Role.STUDENT));

        Course course = createOpenCourse(creator, 10, 0);
        Course savedCourse = courseRepository.save(course);

        Enrollment enrollment = enrollmentRepository.save(
                Enrollment.create(student, savedCourse)
        );

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                try {
                    enrollmentCommandService.confirm(
                            student.getId(),
                            enrollment.getId()
                    );
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        int successCount = 0;
        int failCount = 0;

        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            } else {
                failCount++;
            }
        }

        executorService.shutdown();

        entityManager.clear();

        // Then
        Course resultCourse = courseRepository.findById(savedCourse.getId()).orElseThrow();
        Enrollment resultEnrollment = enrollmentRepository.findById(enrollment.getId()).orElseThrow();

        assertThat(successCount).isEqualTo(1);
        assertThat(failCount).isEqualTo(threadCount - 1);

        assertThat(resultCourse.getCurrentCapacity()).isEqualTo(1);
        assertThat(resultEnrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CONFIRMED);
        assertThat(resultEnrollment.getConfirmedAt()).isNotNull();
    }

    @Test
    @DisplayName("수강 취소 동시성 테스트 - 같은 수강 신청을 동시에 취소해도 정원은 한 번만 감소한다")
    void cancel_concurrency_decrease_capacity_only_once_when_same_enrollment_cancelled() throws Exception {
        // Given
        int threadCount = 100;

        User creator = userRepository.save(User.create("강사", Role.CREATOR));
        User student = userRepository.save(User.create("학생", Role.STUDENT));

        Course course = createOpenCourse(creator, 10, 1);
        Course savedCourse = courseRepository.save(course);

        Enrollment enrollment = Enrollment.create(student, savedCourse);
        enrollment.confirm();
        Enrollment savedEnrollment = enrollmentRepository.save(enrollment);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(() -> {
                readyLatch.countDown();
                startLatch.await();

                try {
                    enrollmentCommandService.cancel(
                            student.getId(),
                            savedEnrollment.getId()
                    );
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }));
        }

        readyLatch.await();
        startLatch.countDown();

        int successCount = 0;
        int failCount = 0;

        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            } else {
                failCount++;
            }
        }

        executorService.shutdown();

        entityManager.clear();

        // Then
        Course resultCourse = courseRepository.findById(savedCourse.getId()).orElseThrow();
        Enrollment resultEnrollment = enrollmentRepository.findById(savedEnrollment.getId()).orElseThrow();

        assertThat(successCount).isEqualTo(1);
        assertThat(failCount).isEqualTo(threadCount - 1);

        assertThat(resultCourse.getCurrentCapacity()).isEqualTo(0);
        assertThat(resultEnrollment.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    @Test
    @DisplayName("수강 취소 동시성 테스트 - 서로 다른 두 사용자가 동시에 취소하면 둘 다 취소되고 정원은 두 번 감소한다")
    void cancel_concurrency_two_different_users_both_success() throws Exception {
        // Given
        int threadCount = 2;

        User creator = userRepository.save(User.create("강사", Role.CREATOR));
        User studentA = userRepository.save(User.create("학생A", Role.STUDENT));
        User studentB = userRepository.save(User.create("학생B", Role.STUDENT));

        Course course = createOpenCourse(creator, 10, 2);
        Course savedCourse = courseRepository.save(course);

        Enrollment enrollmentA = Enrollment.create(studentA, savedCourse);
        enrollmentA.confirm();
        Enrollment savedEnrollmentA = enrollmentRepository.save(enrollmentA);

        Enrollment enrollmentB = Enrollment.create(studentB, savedCourse);
        enrollmentB.confirm();
        Enrollment savedEnrollmentB = enrollmentRepository.save(enrollmentB);

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        List<Future<Boolean>> futures = new ArrayList<>();

        futures.add(executorService.submit(() -> {
            readyLatch.countDown();
            startLatch.await();

            try {
                enrollmentCommandService.cancel(
                        studentA.getId(),
                        savedEnrollmentA.getId()
                );
                return true;
            } catch (Exception e) {
                return false;
            }
        }));

        futures.add(executorService.submit(() -> {
            readyLatch.countDown();
            startLatch.await();

            try {
                enrollmentCommandService.cancel(
                        studentB.getId(),
                        savedEnrollmentB.getId()
                );
                return true;
            } catch (Exception e) {
                return false;
            }
        }));

        readyLatch.await();
        startLatch.countDown();

        int successCount = 0;

        for (Future<Boolean> future : futures) {
            if (future.get()) {
                successCount++;
            }
        }

        executorService.shutdown();

        entityManager.clear();

        // Then
        Course resultCourse = courseRepository.findById(savedCourse.getId()).orElseThrow();
        Enrollment resultEnrollmentA = enrollmentRepository.findById(savedEnrollmentA.getId()).orElseThrow();
        Enrollment resultEnrollmentB = enrollmentRepository.findById(savedEnrollmentB.getId()).orElseThrow();

        assertThat(successCount).isEqualTo(2);

        assertThat(resultCourse.getCurrentCapacity()).isEqualTo(0);

        assertThat(resultEnrollmentA.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
        assertThat(resultEnrollmentB.getEnrollmentStatus()).isEqualTo(EnrollmentStatus.CANCELLED);
    }

    private Course createOpenCourse(User creator, int maxCapacity, int currentCapacity) {
        Course course = Course.create(
                "테스트 강의",
                "테스트 설명",
                10000,
                maxCapacity,
                LocalDate.now(),
                LocalDate.now().plusDays(30),
                creator
        );

        ReflectionTestUtils.setField(course, "currentCapacity", currentCapacity);
        course.open();

        return course;
    }
}
