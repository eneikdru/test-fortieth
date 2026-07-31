package com.eneik.generated.knowledgebase;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final KbUserRepository userRepository;
    private final JwtService jwtService;
    private final String defaultPassword;

    public AuthController(KbUserRepository userRepository,
                          JwtService jwtService,
                          @Value("${auth.default-password:password}") String defaultPassword) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
        this.defaultPassword = defaultPassword;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        String username = request.getUsername();
        String password = request.getPassword();
        String requestedRole = request.getRole();

        if (username == null || username.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required");
        }
        if (password == null || password.trim().isEmpty() || !password.equals(defaultPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }

        KbUser user = userRepository.findByUsername(username.trim())
            .orElseGet(() -> {
                String role = (requestedRole != null && !requestedRole.trim().isEmpty()) ? requestedRole.trim().toUpperCase() : "LEARNER";
                KbUser newUser = new KbUser();
                newUser.setUsername(username.trim());
                newUser.setRole(role);
                return userRepository.save(newUser);
            });

        String token = jwtService.generateToken(user.getUsername(), user.getRole());

        return new LoginResponse(token, user.getUsername(), user.getRole());
    }

    public static class LoginRequest {
        private String username;
        private String password;
        private String role;

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }

    public static class LoginResponse {
        private String token;
        private String username;
        private String role;

        public LoginResponse(String token, String username, String role) {
            this.token = token;
            this.username = username;
            this.role = role;
        }

        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
