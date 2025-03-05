package com.example.centralOperator.service;

import org.springframework.stereotype.Service;

@Service // Marks this class as a Service component
public class GreetingService {

    public String greet(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "Hello, Guest!";
        } else {
            return "Hello, " + name + "!";
        }
    }
}