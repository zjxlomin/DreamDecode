// src/main/java/est/DreamDecode/service/UserService.java
package est.DreamDecode.service;

import est.DreamDecode.domain.User;
import est.DreamDecode.dto.SignupRequest;
import est.DreamDecode.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    @Transactional
    public void signup(SignupRequest req) {
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        String hashed = passwordEncoder.encode(req.getPassword());

        User user = User.builder()
                .email(req.getEmail())
                .password(hashed)
                .name(req.getName())
                .birthday(req.getBirthday())
                .gender(req.getGender())
                .build();

        userRepository.save(user);

        emailVerificationService.sendSignupOtp(user);

    }
}
