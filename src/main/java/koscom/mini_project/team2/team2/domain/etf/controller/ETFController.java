package koscom.mini_project.team2.team2.domain.etf.controller;

import koscom.mini_project.team2.team2.domain.etf.dto.ETFCreateRequest;
import koscom.mini_project.team2.team2.domain.etf.dto.EtfResponse;
import koscom.mini_project.team2.team2.domain.etf.service.EtfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/etf")
public class ETFController {

    private final EtfService etfService;

    @GetMapping("/test")
    public String test() {
        return "Hello World";
    }

    @PostMapping
    public ResponseEntity<EtfResponse> create(@RequestBody ETFCreateRequest request) {
        EtfResponse created = etfService.create(request);
        return ResponseEntity
                .created(URI.create("/etf/" + created.id()))
                .body(created);
    }

    // Read one
    @GetMapping("/{id}")
    public ResponseEntity<EtfResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(etfService.findById(id));
    }

    // Read all
    @GetMapping
    public ResponseEntity<List<EtfResponse>> findAll() {
        return ResponseEntity.ok(etfService.findAll());
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        etfService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
