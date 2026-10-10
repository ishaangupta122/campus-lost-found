package com.campus.lostandfound.exception;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

/**
 * Tests the error handler alone. A tiny fake controller throws each kind of
 * exception, and we check the HTTP status and the JSON message that comes back.
 */
class GlobalExceptionHandlerTest {

    record Body(@NotBlank(message = "Name is required") String name) {
    }

    @RestController
    static class ThrowingController {

        @GetMapping("/test/not-found")
        String notFound() {
            throw new ResourceNotFoundException("Item 5 not found");
        }

        @GetMapping("/test/bad-request")
        String badRequest() {
            throw new BadRequestException("Bad input");
        }

        @GetMapping("/test/forbidden")
        String forbidden() {
            throw new ForbiddenException("Not allowed");
        }

        @GetMapping("/test/conflict")
        String conflict() {
            throw new ConflictException("Already exists");
        }

        @GetMapping("/test/unauthorized")
        String unauthorized() {
            throw new UnauthorizedException("Login first");
        }

        @GetMapping("/test/integrity")
        String integrity() {
            throw new DataIntegrityViolationException("duplicate key secret-details");
        }

        @GetMapping("/test/boom")
        String boom() {
            throw new IllegalStateException("secret internal detail");
        }

        @GetMapping("/test/number/{id}")
        String number(@PathVariable Long id) {
            return "ok";
        }

        @PostMapping("/test/body")
        String body(@Valid @RequestBody Body body) {
            return "ok";
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ThrowingController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void customExceptionsMapToTheirStatusCodes() throws Exception {
        mockMvc.perform(get("/test/not-found")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Item 5 not found"))
                .andExpect(jsonPath("$.status").value(404));
        mockMvc.perform(get("/test/bad-request")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/test/forbidden")).andExpect(status().isForbidden());
        mockMvc.perform(get("/test/conflict")).andExpect(status().isConflict());
        mockMvc.perform(get("/test/unauthorized")).andExpect(status().isUnauthorized());
    }

    @Test
    void databaseRuleViolationIsAConflictAndHidesDetails() throws Exception {
        mockMvc.perform(get("/test/integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("This data conflicts with an existing record"));
    }

    @Test
    void unexpectedErrorsBecomeGeneric500WithoutLeakingDetails() throws Exception {
        mockMvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Something went wrong on the server"));
    }

    @Test
    void invalidPathValueIsBadRequest() throws Exception {
        mockMvc.perform(get("/test/number/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'id'"));
    }

    @Test
    void validationErrorsListEachField() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.fieldErrors.name").value("Name is required"));
    }

    @Test
    void malformedJsonIsBadRequest() throws Exception {
        mockMvc.perform(post("/test/body").contentType(MediaType.APPLICATION_JSON).content("{not json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void wrongHttpMethodIs405() throws Exception {
        mockMvc.perform(post("/test/not-found")).andExpect(status().isMethodNotAllowed());
    }
}
