package com.lms.auth_service.controller;

import com.lms.auth_service.entity.User;
import com.lms.auth_service.service.AdminUserService;
import com.lms.auth_service.config.JwtAuthenticationFilter;
import com.lms.auth_service.util.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
@AutoConfigureMockMvc(addFilters = false)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminUserService adminUserService;

    @MockBean
    private JwtUtil jwtUtil;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getAllUsers_success() throws Exception {
        User user = new User();
        user.setUserId(1);
        user.setEmail("admin@lms.com");

        when(adminUserService.getAllUsers()).thenReturn(List.of(user));

        mockMvc.perform(get("/api/v1/admin/users/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("admin@lms.com"));
    }

    @Test
    void deleteUser_success() throws Exception {
        doNothing().when(adminUserService).deleteUser(1);

        mockMvc.perform(delete("/api/v1/admin/users/1"))
                .andExpect(status().isNoContent());
        
        verify(adminUserService).deleteUser(1);
    }
}
