package com.example.demo.api.service;

import com.example.demo.api.dto.UserCreateDto;
import com.example.demo.api.persistence.entity.Role;
import com.example.demo.api.persistence.entity.User;
import com.example.demo.api.persistence.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 시 이름과 역할을 기반으로 User 엔티티를 생성하고 저장한다")
    void signUp_success() {
        // GIVEN
        UserCreateDto dto = new UserCreateDto("홍길동", Role.CREATOR);

        // WHEN
        userService.signUp(dto);

        // THEN
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getName()).isEqualTo("홍길동");
        assertThat(savedUser.getRole()).isEqualTo(Role.CREATOR);
    }
}
