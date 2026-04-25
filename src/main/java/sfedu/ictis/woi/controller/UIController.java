package sfedu.ictis.woi.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import sfedu.ictis.woi.service.JwtService;

@Controller
@RequiredArgsConstructor
public class UIController {
    private final JwtService jwtService;

    @GetMapping("/panel")
    public String getPanel(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        String token = jwtService.generateToken(userDetails);

        model.addAttribute("jwtToken", token);

        return "panel";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }
}