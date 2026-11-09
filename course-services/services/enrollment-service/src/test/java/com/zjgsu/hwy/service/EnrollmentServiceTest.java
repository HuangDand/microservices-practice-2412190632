package com.zjgsu.hwy.service;

import com.zjgsu.hwy.client.CatalogClient;
import com.zjgsu.hwy.client.StudentClient;
import com.zjgsu.hwy.model.Enrollment;
import com.zjgsu.hwy.model.EnrollmentStatus;
import com.zjgsu.hwy.repository.EnrollmentRepository;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.ResourceAccessException;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnrollmentServiceTest {

    @Mock
    private EnrollmentRepository enrollmentRepository;
    @Mock
    private StudentClient studentClient;
    @Mock
    private CatalogClient catalogClient;

    private EnrollmentService service;

    private static final String STUDENT_ID = "student-001";
    private static final String COURSE_ID = "course-001";

    private static final Request FEIGN_REQUEST =
            Request.create(Request.HttpMethod.GET, "/test", Map.of(), null, null, null);

    @BeforeEach
    void setUp() {
        service = new EnrollmentService(enrollmentRepository, studentClient, catalogClient);
    }

    /** 构造带指定状态码的 Feign 异常（404/409/5xx） */
    private static FeignException feignStatus(int status) {
        return FeignException.errorStatus("test",
                Response.builder().status(status).request(FEIGN_REQUEST).headers(Map.of()).build());
    }

    /** 构造连接失败/读取超时类异常 */
    private static RetryableException retryable(String message) {
        return new RetryableException(-1, message, Request.HttpMethod.GET, null,
                (Long) null, FEIGN_REQUEST);
    }

    private void givenStudentOk() {
        when(studentClient.findById(STUDENT_ID)).thenReturn(Map.of("id", STUDENT_ID));
    }

    private void givenCourseOk() {
        when(catalogClient.findById(COURSE_ID)).thenReturn(Map.of("id", COURSE_ID));
    }

    @Test
    void enroll_正常选课_按顺序校验学生_查询课程_占用名额_保存记录() {
        givenStudentOk();
        givenCourseOk();
        when(enrollmentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(false);
        Enrollment saved = new Enrollment();
        saved.setId(UUID.randomUUID().toString());
        saved.setStudentId(STUDENT_ID);
        saved.setCourseId(COURSE_ID);
        saved.setStatus(EnrollmentStatus.ACTIVE);
        when(enrollmentRepository.save(any(Enrollment.class))).thenReturn(saved);

        Enrollment result = service.enroll(STUDENT_ID, COURSE_ID);

        assertNotNull(result.getId());
        assertEquals(STUDENT_ID, result.getStudentId());
        assertEquals(COURSE_ID, result.getCourseId());
        assertEquals(EnrollmentStatus.ACTIVE, result.getStatus());

        var order = inOrder(studentClient, catalogClient, enrollmentRepository);
        order.verify(studentClient).findById(STUDENT_ID);
        order.verify(catalogClient).findById(COURSE_ID);
        order.verify(enrollmentRepository).existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID);
        order.verify(catalogClient).occupy(COURSE_ID);
        order.verify(enrollmentRepository).save(any(Enrollment.class));
    }

    @Test
    void enroll_学生不存在_返回404语义且不占用名额() {
        when(studentClient.findById(STUDENT_ID)).thenThrow(feignStatus(404));

        assertThrows(NotFoundException.class, () -> service.enroll(STUDENT_ID, COURSE_ID));
        verifyNoInteractions(catalogClient);
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enroll_重复选课_409语义且不占用名额() {
        givenStudentOk();
        givenCourseOk();
        when(enrollmentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(true);

        assertThrows(ConflictException.class, () -> service.enroll(STUDENT_ID, COURSE_ID));
        verify(catalogClient, never()).occupy(anyString());
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enroll_课程已满_保留409业务错误且不保存() {
        givenStudentOk();
        givenCourseOk();
        when(enrollmentRepository.existsByStudentIdAndCourseId(STUDENT_ID, COURSE_ID)).thenReturn(false);
        doThrow(feignStatus(409)).when(catalogClient).occupy(COURSE_ID);

        assertThrows(ConflictException.class, () -> service.enroll(STUDENT_ID, COURSE_ID));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enroll_依赖不可用_503语义且不保存() {
        when(studentClient.findById(STUDENT_ID)).thenThrow(retryable("Connection refused"));

        assertThrows(ResourceAccessException.class, () -> service.enroll(STUDENT_ID, COURSE_ID));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void enroll_下游5xx_返回503而非学生不存在() {
        when(studentClient.findById(STUDENT_ID)).thenThrow(feignStatus(503));

        assertThrows(ResourceAccessException.class, () -> service.enroll(STUDENT_ID, COURSE_ID));
    }

    @Test
    void enroll_学生服务返回无效响应_503语义() {
        when(studentClient.findById(STUDENT_ID)).thenReturn(Map.of("id", "someone-else"));

        assertThrows(ResourceAccessException.class, () -> service.enroll(STUDENT_ID, COURSE_ID));
        verify(catalogClient, never()).findById(anyString());
    }

    @Test
    void enroll_读取超时_503语义且不保存_远端可能已占用但未保存本地记录() {
        // 读调用超时：远端可能尚未执行，也可能已执行但响应丢失
        when(studentClient.findById(STUDENT_ID)).thenThrow(retryable("Read timed out"));

        assertThrows(ResourceAccessException.class, () -> service.enroll(STUDENT_ID, COURSE_ID));
        verify(enrollmentRepository, never()).save(any());
    }

    @Test
    void withdraw_正常退课_先释放名额再删除记录() {
        Enrollment enrollment = new Enrollment();
        enrollment.setId("enroll-001");
        enrollment.setCourseId(COURSE_ID);
        when(enrollmentRepository.findById("enroll-001")).thenReturn(Optional.of(enrollment));

        service.withdraw("enroll-001");

        var order = inOrder(catalogClient, enrollmentRepository);
        order.verify(catalogClient).release(COURSE_ID);
        order.verify(enrollmentRepository).deleteById("enroll-001");
    }

    @Test
    void withdraw_课程服务不可用_不删除本地记录() {
        Enrollment enrollment = new Enrollment();
        enrollment.setId("enroll-001");
        enrollment.setCourseId(COURSE_ID);
        when(enrollmentRepository.findById("enroll-001")).thenReturn(Optional.of(enrollment));
        doThrow(retryable("Connection refused")).when(catalogClient).release(COURSE_ID);

        assertThrows(ResourceAccessException.class, () -> service.withdraw("enroll-001"));
        verify(enrollmentRepository, never()).deleteById(anyString());
    }

    @Test
    void withdraw_选课记录不存在_404语义且不调用课程服务() {
        when(enrollmentRepository.findById("enroll-999")).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.withdraw("enroll-999"));
        verifyNoInteractions(catalogClient);
        verify(enrollmentRepository, never()).deleteById(any());
    }
}
