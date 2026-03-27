package com.rianlucassb.liftform.presentation.controller;

import com.rianlucassb.liftform.presentation.constants.ApiPaths;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.V1 + "/test")
public class TestController {
    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }
}
