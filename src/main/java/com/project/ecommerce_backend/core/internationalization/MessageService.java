package com.project.ecommerce_backend.core.internationalization;

public interface MessageService {
    String getMessage(String key);
    String getMessageWithParams(String keyword, Object... params);
}
