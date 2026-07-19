package com.vointika.shared.web.security;

import org.springframework.http.HttpMethod;

public record PublicRoute(HttpMethod method, String pattern) {}
