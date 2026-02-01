package koscom.mini_project.team2.team2.domain.dummy.service;

import jakarta.persistence.EntityNotFoundException;
import koscom.mini_project.team2.team2.domain.dummy.dto.DummyCreateRequest;
import koscom.mini_project.team2.team2.domain.dummy.dto.DummyResponse;
import koscom.mini_project.team2.team2.domain.dummy.dto.DummyUpdateRequest;
import koscom.mini_project.team2.team2.domain.dummy.entity.Dummy;
import koscom.mini_project.team2.team2.domain.dummy.repository.DummyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class DummyService {

    private final DummyRepository dummyRepository;

    public DummyService(DummyRepository dummyRepository) {
        this.dummyRepository = dummyRepository;
    }

    public DummyResponse create(DummyCreateRequest request) {
        Dummy dummy = Dummy.builder().name(request.name()).age(request.age()).build();
        Dummy saved = dummyRepository.save(dummy);
        return DummyResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public DummyResponse findById(Long id) {
        Dummy dummy = dummyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dummy not found. id=" + id));
        return DummyResponse.from(dummy);
    }

    @Transactional(readOnly = true)
    public List<DummyResponse> findAll() {
        return dummyRepository.findAll().stream()
                .map(DummyResponse::from)
                .toList();
    }

    public DummyResponse update(Long id, DummyUpdateRequest request) {
        Dummy dummy = dummyRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Dummy not found. id=" + id));

        dummy.update(request.name(), request.age()); // dirty checking
        return DummyResponse.from(dummy);
    }

    public void delete(Long id) {
        if (!dummyRepository.existsById(id)) {
            throw new EntityNotFoundException("Dummy not found. id=" + id);
        }
        dummyRepository.deleteById(id);
    }
}