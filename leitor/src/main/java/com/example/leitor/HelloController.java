package com.example.leitor;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {

    @GetMapping("/teste/")
    public String index() {
        return "Greetings from Spring Boot!";
    }

}