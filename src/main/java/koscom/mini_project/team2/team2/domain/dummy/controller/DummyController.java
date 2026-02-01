package koscom.mini_project.team2.team2.domain.dummy.controller;

import koscom.mini_project.team2.team2.domain.dummy.dto.DummyCreateRequest;
import koscom.mini_project.team2.team2.domain.dummy.dto.DummyResponse;
import koscom.mini_project.team2.team2.domain.dummy.dto.DummyUpdateRequest;
import koscom.mini_project.team2.team2.domain.dummy.service.DummyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/dummy")
public class DummyController {

    private final DummyService dummyService;

    @GetMapping("/test")
    public String test() {
        return "Hello World";
    }

    @PostMapping
    public ResponseEntity<DummyResponse> create(@RequestBody DummyCreateRequest request) {
        DummyResponse created = dummyService.create(request);
        return ResponseEntity
                .created(URI.create("/api/dummies/" + created.id()))
                .body(created);
    }

    // Read one
    @GetMapping("/{id}")
    public ResponseEntity<DummyResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(dummyService.findById(id));
    }

    // Read all
    @GetMapping
    public ResponseEntity<List<DummyResponse>> findAll() {
        return ResponseEntity.ok(dummyService.findAll());
    }

    // Update
    @PutMapping("/{id}")
    public ResponseEntity<DummyResponse> update(@PathVariable Long id,
                                                @RequestBody DummyUpdateRequest request) {
        return ResponseEntity.ok(dummyService.update(id, request));
    }

    // Delete
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        dummyService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
