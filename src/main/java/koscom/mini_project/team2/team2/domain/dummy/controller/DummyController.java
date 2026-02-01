package koscom.mini_project.team2.team2.domain.dummy.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/dummy")
public class DummyController {

    @GetMapping("/test")
    public String test() {
        return "Hello World";
    }

}
