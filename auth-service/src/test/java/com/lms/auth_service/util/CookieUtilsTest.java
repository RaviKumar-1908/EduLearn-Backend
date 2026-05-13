package com.lms.auth_service.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Serializable;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CookieUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Test
    void getCookie_exists_returnsCookie() {
        Cookie cookie = new Cookie("test-cookie", "test-value");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        Optional<Cookie> result = CookieUtils.getCookie(request, "test-cookie");

        assertTrue(result.isPresent());
        assertEquals("test-value", result.get().getValue());
    }

    @Test
    void getCookie_notExists_returnsEmpty() {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("other", "val")});

        Optional<Cookie> result = CookieUtils.getCookie(request, "test-cookie");

        assertFalse(result.isPresent());
    }

    @Test
    void addCookie_addsToResponse() {
        CookieUtils.addCookie(response, "new-cookie", "new-val", 3600);

        verify(response).addCookie(argThat(cookie -> 
            cookie.getName().equals("new-cookie") && 
            cookie.getValue().equals("new-val") &&
            cookie.getMaxAge() == 3600
        ));
    }

    @Test
    void deleteCookie_resetsCookieInResponse() {
        Cookie cookie = new Cookie("del-cookie", "del-val");
        when(request.getCookies()).thenReturn(new Cookie[]{cookie});

        CookieUtils.deleteCookie(request, response, "del-cookie");

        verify(response).addCookie(argThat(c -> 
            c.getName().equals("del-cookie") && 
            c.getValue().isEmpty() &&
            c.getMaxAge() == 0
        ));
    }

    @Test
    void serialize_deserialize_success() {
        TestObject obj = new TestObject("Hello World");
        
        String serialized = CookieUtils.serialize(obj);
        assertNotNull(serialized);

        Cookie cookie = new Cookie("test", serialized);
        TestObject deserialized = CookieUtils.deserialize(cookie, TestObject.class);

        assertEquals("Hello World", deserialized.getName());
    }

    static class TestObject implements Serializable {
        private String name;
        public TestObject() {}
        public TestObject(String name) { this.name = name; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
    }
}
