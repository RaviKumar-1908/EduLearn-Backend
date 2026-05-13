package com.lms.auth_service.config;

import com.lms.auth_service.entity.User;
import com.lms.auth_service.enums.ApprovalStatus;
import com.lms.auth_service.enums.AuthProvider;
import com.lms.auth_service.enums.Role;
import com.lms.auth_service.repository.UserRepository;
import com.lms.auth_service.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;
    private final org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate;

    @Value("${application.oauth2.redirect-uri}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        log.info("OAuth2 login attributes: {}", oAuth2User.getAttributes());

        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String givenName = oAuth2User.getAttribute("given_name");
        String familyName = oAuth2User.getAttribute("family_name");
        String picture = oAuth2User.getAttribute("picture");

        if (name == null || name.isBlank()) {
            if (givenName != null && !givenName.isBlank()) {
                name = givenName + (familyName != null ? " " + familyName : "");
            } else {
                name = email != null ? email.split("@")[0] : "Learner";
            }
        }

        if (email == null || email.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Email not found from Google");
            return;
        }

        if (picture == null || picture.isBlank()) {
            picture = "https://ui-avatars.com/api/?name=" + URLEncoder.encode(name, StandardCharsets.UTF_8) + "&background=random";
        }

        HttpSession session = request.getSession(false);
        Role selectedRole = Role.STUDENT;
        if (session != null) {
            Object roleObj = session.getAttribute(CustomOAuth2AuthorizationRequestResolver.GOOGLE_SELECTED_ROLE);
            if (roleObj != null) {
                try {
                    selectedRole = Role.valueOf(roleObj.toString().toUpperCase());
                } catch (IllegalArgumentException e) {
                    selectedRole = Role.STUDENT;
                }
            }
        }

        if (selectedRole == Role.ADMIN) {
            String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", URLEncoder.encode("Admin role is not allowed for Google authentication.", StandardCharsets.UTF_8))
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;

        if (userOptional.isPresent()) {
            user = userOptional.get();
            // Update profile
            user.setFullName(name);
            user.setProfilePicUrl(picture);
            user.setProvider(AuthProvider.GOOGLE);
            user = userRepository.save(user);
        } else {
            // New user flow - Always set to APPROVED as per new requirement
            user = User.builder()
                    .email(email)
                    .fullName(name)
                    .profilePicUrl(picture)
                    .provider(AuthProvider.GOOGLE)
                    .role(selectedRole)
                    .approvalStatus(ApprovalStatus.APPROVED)
                    .active(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            user = userRepository.save(user);
            
            // Publish registration event for welcome email
            java.util.Map<String, Object> regEvent = new java.util.HashMap<>();
            regEvent.put("userId", user.getUserId());
            regEvent.put("email", user.getEmail());
            regEvent.put("fullName", user.getFullName());
            regEvent.put("title", "Welcome to our Platform!");
            regEvent.put("message", "Welcome " + user.getFullName() + "! We are glad to have you on board via Google.");
            regEvent.put("type", "AUTH_REGISTER");
            rabbitTemplate.convertAndSend("lms.events.exchange", "notification.auth.register", regEvent);
        }

        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole().name(), user.getFullName(), user.getProfilePicUrl(), user.getGender());

        // Redirect to frontend with token and user details
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("token", token)
                .queryParam("userId", user.getUserId())
                .queryParam("email", user.getEmail())
                .queryParam("fullName", user.getFullName())
                .queryParam("role", user.getRole().name())
                .queryParam("approvalStatus", user.getApprovalStatus() != null ? user.getApprovalStatus().name() : "APPROVED")
                .build().toUriString();

        // Publish login event for notification
        java.util.Map<String, Object> loginEvent = new java.util.HashMap<>();
        loginEvent.put("userId", user.getUserId());
        loginEvent.put("email", user.getEmail());
        loginEvent.put("title", "New Google Login Detected");
        loginEvent.put("message", "Hello " + user.getFullName() + ", you just logged into EduLearn via Google. If this wasn't you, please secure your account.");
        loginEvent.put("type", "AUTH_LOGIN");
        rabbitTemplate.convertAndSend("lms.events.exchange", "notification.auth.login", loginEvent);

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        cookieAuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}
