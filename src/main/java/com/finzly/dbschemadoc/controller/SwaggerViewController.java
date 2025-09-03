package com.finzly.dbschemadoc.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller to serve custom Swagger UI with vertical text fix.
 */
@Controller
public class SwaggerViewController {

    @GetMapping("/")
    public String homePage() {
        return "forward:/swagger-ui-custom.html";
    }

    @GetMapping("/swagger-ui.html")
    public String defaultSwaggerUI() {
        return "forward:/swagger-ui-custom.html";
    }

    @GetMapping("/docs")
    public String customSwaggerUI() {
        return "forward:/swagger-ui-custom.html";
    }
    
    @GetMapping("/api-docs")
    public String alternativeSwaggerUI() {
        return "forward:/swagger-ui-custom.html";
    }
    
    @GetMapping("/swagger")
    public String swaggerAlias() {
        return "forward:/swagger-ui-custom.html";
    }
}
