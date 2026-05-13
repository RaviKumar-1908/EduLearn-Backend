package com.lms.auth_service.config;

import com.lms.auth_service.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String GOOGLE_SELECTED_ROLE = "GOOGLE_SELECTED_ROLE";
    public static final String GOOGLE_AUTH_MODE = "GOOGLE_AUTH_MODE";

    private final DefaultOAuth2AuthorizationRequestResolver defaultResolver;

    public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.defaultResolver =
                new DefaultOAuth2AuthorizationRequestResolver(
                        clientRegistrationRepository,
                        "/oauth2/authorization"
                );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request);
        return customize(request, authorizationRequest);
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authorizationRequest = defaultResolver.resolve(request, clientRegistrationId);
        return customize(request, authorizationRequest);
    }

    private OAuth2AuthorizationRequest customize(
            HttpServletRequest request,
            OAuth2AuthorizationRequest authorizationRequest
    ) {
        if (authorizationRequest == null) {
            return null;
        }

        String mode = request.getParameter("mode");
        String roleParam = request.getParameter("role");

        HttpSession session = request.getSession(true);

        if (mode != null && !mode.isBlank()) {
            session.setAttribute(GOOGLE_AUTH_MODE, mode.toLowerCase());
        } else {
            session.setAttribute(GOOGLE_AUTH_MODE, "login");
        }

        Role selectedRole = Role.STUDENT; // Default
        if (roleParam != null && !roleParam.isBlank()) {
            try {
                Role role = Role.valueOf(roleParam.toUpperCase());
                if (role == Role.ADMIN) {
                    selectedRole = Role.STUDENT; // Block admin signup via Google
                } else {
                    selectedRole = role;
                }
            } catch (IllegalArgumentException e) {
                selectedRole = Role.STUDENT;
            }
        }
        session.setAttribute(GOOGLE_SELECTED_ROLE, selectedRole.name());

        java.util.Map<String, Object> additionalParameters = new java.util.HashMap<>(authorizationRequest.getAdditionalParameters());
        additionalParameters.put("prompt", "select_account");

        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .build();
    }
}
