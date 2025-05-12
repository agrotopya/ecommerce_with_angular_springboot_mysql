package com.fibiyo.ecommerce.infrastructure.security;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@AuthenticationPrincipal(expression = "user") // user alanını işaret etmesi için expression eklendi
public @interface CurrentUser {
}
