package com.aditya.jobtracker.controller;

import com.aditya.jobtracker.dto.MeResponse;
import com.aditya.jobtracker.dto.RegisterRequest;
import com.aditya.jobtracker.exception.DuplicateUserException;
import com.aditya.jobtracker.model.AppUser;
import com.aditya.jobtracker.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    // POST /api/auth/register  (open to everyone, see SecurityConfig)
    @PostMapping("/register")
    public ResponseEntity<MeResponse> register(@RequestBody RegisterRequest req)
            throws DuplicateUserException {
        AppUser user = userService.register(req.username(), req.password());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new MeResponse(user.getUsername(), user.getRole()));
    }

    // GET /api/auth/me  (needs login). The frontend will use this as its "login check".
    // Spring gives us the logged-in person as the Authentication parameter.
    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        String authority = authentication.getAuthorities().iterator().next().getAuthority();
        return new MeResponse(authentication.getName(), authority.replace("ROLE_", ""));
    }
}
