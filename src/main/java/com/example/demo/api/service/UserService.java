package com.example.demo.api.service;

import com.example.demo.api.dto.UserCreateDto;
import com.example.demo.api.persistence.entity.User;
import com.example.demo.api.persistence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public void signUp(UserCreateDto userCreateDto) {
        final User user = User.create(userCreateDto.name(), userCreateDto.role());
        userRepository.save(user);
    }
}
