package com.danchi.admin;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LegacyAdminController {
    @GetMapping({"/supersecret", "/supersecret/", "/supersecret/**"})
    public String legacyAdmin() {
        return "redirect:/admin/";
    }
}
