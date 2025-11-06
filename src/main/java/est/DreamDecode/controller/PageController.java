package est.DreamDecode.controller;

import org.springframework.stereotype.Controller;
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
    public String profile() {
        return "profile"; // templates/profile.html
    }
}
