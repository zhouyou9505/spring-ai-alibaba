//package com.rowboat.agents.config;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.ArrayList;
//
//public class ApiKeyAuthFilter extends OncePerRequestFilter {
//
//    private final String apiKey;
//
//    public ApiKeyAuthFilter(String apiKey) {
//        this.apiKey = apiKey;
//    }
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
//            throws ServletException, IOException {
//
//        String authHeader = request.getHeader("Authorization");
//
//        if (authHeader != null && authHeader.startsWith("Bearer ")) {
//            String token = authHeader.substring(7);
//
//            if (apiKey != null && !apiKey.trim().isEmpty() && apiKey.equals(token)) {
//                UsernamePasswordAuthenticationToken authentication =
//                    new UsernamePasswordAuthenticationToken("user", null, new ArrayList<>());
//                SecurityContextHolder.getContext().setAuthentication(authentication);
//            }
//        }
//
//        filterChain.doFilter(request, response);
//    }
//}