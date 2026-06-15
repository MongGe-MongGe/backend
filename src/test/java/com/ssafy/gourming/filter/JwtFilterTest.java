package com.ssafy.gourming.filter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.ssafy.gourming.util.JwtUtil;

import jakarta.servlet.FilterChain;

class JwtFilterTest {

    private final JwtUtil jwtUtil = mock(JwtUtil.class);
    private final JwtFilter jwtFilter = new JwtFilter(jwtUtil);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void validTokenStoresUserIdAsPrincipal() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);
        request.addHeader("Authorization", "Bearer valid-token");

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn("user-1");

        jwtFilter.doFilter(request, response, filterChain);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertEquals("user-1", authentication.getPrincipal());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void requestWithoutTokenDoesNotSetAuthentication() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtFilter.doFilter(request, response, filterChain);

        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(filterChain).doFilter(request, response);
    }
}
