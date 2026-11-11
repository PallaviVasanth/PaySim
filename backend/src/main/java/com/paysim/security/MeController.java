package com.paysim.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Session")
public class MeController {

    public record CurrentUser(String username, List<String> roles) {
    }

    @GetMapping
    @Operation(summary = "Who am I? Used by the UI to verify credentials and hide actions the role cannot perform")
    public CurrentUser me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
                .map(a -> a.getAuthority().replaceFirst("^ROLE_", ""))
                .sorted()
                .toList();
        return new CurrentUser(authentication.getName(), roles);
    }
}
