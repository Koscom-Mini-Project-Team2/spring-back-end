package koscom.mini_project.team2.team2.domain.news;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsController {

    private final NaverNewsService naverNewsService;

    @GetMapping
    public String getNews(@RequestParam String keyword) {
        return naverNewsService.searchNews(keyword);
    }
}