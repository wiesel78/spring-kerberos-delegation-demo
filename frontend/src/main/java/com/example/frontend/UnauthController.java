package com.example.frontend;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/unauth")
public class UnauthController {
    @RequestMapping("/test")
    public String test() {
        return "test";
    }
}
