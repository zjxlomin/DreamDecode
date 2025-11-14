package est.DreamDecode.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PageController {

    /** 메인 페이지 */
    @GetMapping("/")
    public String home() {
        return "index"; // templates/index.html
    }

    /** 내 정보 페이지 */
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        model.addAttribute("profile", null);
        return "profile"; // templates/profile.html
    }
}
