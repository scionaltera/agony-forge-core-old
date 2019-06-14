package com.agonyengine.forge.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpSession;

@Controller
public class PublicController {
    @RequestMapping("/public/privacy")
    public String privacy() {
        return "privacy";
    }

    @RequestMapping("/play")
    public String play(@SuppressWarnings("unused") HttpSession session) {
        return "play";
    }
}
