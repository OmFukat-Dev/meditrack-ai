package com.meditrack.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

class AuthenticatedRequestWrapper extends HttpServletRequestWrapper {

    private final Map<String, String> headers = new HashMap<>();

    AuthenticatedRequestWrapper(HttpServletRequest request) {
        super(request);
    }

    void setHeader(String name, String value) {
        if (value != null) {
            headers.put(name, value);
        }
    }

    @Override
    public String getHeader(String name) {
        if (headers.containsKey(name)) {
            return headers.get(name);
        }
        return super.getHeader(name);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        Set<String> names = new HashSet<>(headers.keySet());
        Enumeration<String> original = super.getHeaderNames();
        while (original.hasMoreElements()) {
            names.add(original.nextElement());
        }
        return Collections.enumeration(names);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        if (headers.containsKey(name)) {
            return Collections.enumeration(Collections.singletonList(headers.get(name)));
        }
        return super.getHeaders(name);
    }
}
