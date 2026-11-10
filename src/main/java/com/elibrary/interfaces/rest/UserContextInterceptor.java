package com.elibrary.interfaces.rest;

import com.elibrary.domain.exception.DomainException;
import com.elibrary.domain.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class UserContextInterceptor implements HandlerInterceptor {

    public static final String USER_HEADER = "X-User-Id";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String userId = request.getHeader(USER_HEADER);
        if (!StringUtils.hasText(userId)) {
            throw new DomainException(ErrorCode.UNAUTHORIZED,
                    "Missing required header " + USER_HEADER);
        }
        UserContext.set(userId.trim());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}