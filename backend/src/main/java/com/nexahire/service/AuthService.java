package com.nexahire.service;

import com.nexahire.dto.AuthRequest;
import com.nexahire.dto.AuthResponse;
import com.nexahire.dto.RegisterRequest;
import com.nexahire.entity.CandidateProfile;
import com.nexahire.entity.RecruiterProfile;
import com.nexahire.entity.Role;
import com.nexahire.entity.User;
import com.nexahire.exception.BadRequestException;
import com.nexahire.exception.ResourceNotFoundException;
import com.nexahire.repository.CandidateProfileRepository;
import com.nexahire.repository.RecruiterProfileRepository;
import com.nexahire.repository.RoleRepository;
import com.nexahire.repository.UserRepository;
import com.nexahire.security.JwtService;
import com.nexahire.security.SecurityUtils;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CandidateProfileRepository candidateProfileRepository;
    private final RecruiterProfileRepository recruiterProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final AuditService auditService;
    private final NotificationService notificationService;

    public AuthService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       CandidateProfileRepository candidateProfileRepository,
                       RecruiterProfileRepository recruiterProfileRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       UserDetailsService userDetailsService,
                       AuditService auditService,
                       NotificationService notificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.candidateProfileRepository = candidateProfileRepository;
        this.recruiterProfileRepository = recruiterProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.auditService = auditService;
        this.notificationService = notificationService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new BadRequestException("Username is already taken: " + request.getUsername());
        }

        String roleName = request.getRoleName();
        if (roleName == null || roleName.isBlank()) {
            roleName = "ROLE_CANDIDATE";
        }
        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName.toUpperCase();
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new BadRequestException("Invalid role specified: " + request.getRoleName()));

        User user = new User(
                request.getEmail(),
                request.getUsername(),
                passwordEncoder.encode(request.getPassword()),
                request.getFullName(),
                request.getPhone(),
                role
        );
        user = userRepository.save(user);

        if ("ROLE_CANDIDATE".equals(role.getName())) {
            CandidateProfile candidate = new CandidateProfile(user);
            candidate.setHeadline(request.getHeadline());
            candidate.setSkills(request.getSkills());
            candidateProfileRepository.save(candidate);
        } else if ("ROLE_RECRUITER".equals(role.getName()) || "ROLE_HR_MANAGER".equals(role.getName())) {
            RecruiterProfile recruiter = new RecruiterProfile(
                    user,
                    request.getDepartment() != null ? request.getDepartment() : "Talent Acquisition",
                    request.getDesignation() != null ? request.getDesignation() : "Recruiter",
                    request.getCompanyName() != null ? request.getCompanyName() : "NexaHire Enterprise"
            );
            recruiterProfileRepository.save(recruiter);
        }

        auditService.log("USER_REGISTER", "USER", user.getId(), "User registered with role: " + role.getName());

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails, user.getId(), user.getRole().getName(), user.getFullName());

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole().getName());
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsernameOrEmail())
                .or(() -> userRepository.findByEmail(request.getUsernameOrEmail()))
                .orElseThrow(() -> new BadRequestException("Invalid username or password"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BadRequestException("Account is currently " + user.getStatus() + ". Please contact administrator.");
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(user.getUsername(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getUsername());
        String token = jwtService.generateToken(userDetails, user.getId(), user.getRole().getName(), user.getFullName());

        auditService.log("USER_LOGIN", "USER", user.getId(), "User logged in successfully");

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getEmail(), user.getFullName(), user.getRole().getName());
    }

    public User getCurrentUser() {
        String username = SecurityUtils.getCurrentUsername();
        if (username == null) {
            throw new BadRequestException("No authenticated user found in session");
        }
        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + username));
    }

    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("No account found with email: " + email));

        String token = UUID.randomUUID().toString();
        user.setResetToken(token);
        user.setResetTokenExpiry(LocalDateTime.now().plusHours(2));
        userRepository.save(user);

        String resetLink = "http://localhost:8080/login.html?token=" + token;
        notificationService.sendEmailNotification(
                user.getEmail(),
                "Password Reset Request",
                "Hello " + user.getFullName() + ",\n\nYou requested a password reset for your NexaHire account.\n" +
                "Use the following reset token or link within 2 hours:\n" +
                "Token: " + token + "\nLink: " + resetLink
        );

        auditService.log("PASSWORD_RESET_REQUEST", "USER", user.getId(), "Password reset requested for " + user.getEmail());
    }

    public void resetPassword(String token, String newPassword) {
        User user = userRepository.findByResetToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset token"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset token has expired");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);

        auditService.log("PASSWORD_RESET_SUCCESS", "USER", user.getId(), "Password reset completed successfully");
    }
}
