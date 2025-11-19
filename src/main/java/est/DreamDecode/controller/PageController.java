package est.DreamDecode.controller;

import est.DreamDecode.dto.DreamResponse;
import est.DreamDecode.service.DreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class PageController {

    private final DreamService dreamService;

    /** 메인 페이지 */
    @GetMapping("/")
    public String home() {
        return "index"; // templates/index.html
    }

    /** 공용 꿈 목록 페이지 */
    @GetMapping("/dream")
    public String getPublicDreams(Model model) {
        Page<DreamResponse> dreamPage = dreamService.getAllPublicDreams(0);
        model.addAttribute("dreams", dreamPage.getContent());
        model.addAttribute("hasMore", dreamPage.hasNext());
        return "dreams";
    }

    /** 내 정보 페이지 */
    @GetMapping("/profile")
    public String profile(Authentication authentication, Model model) {
        model.addAttribute("profile", null);
        return "profile"; // templates/profile.html
    }
}
