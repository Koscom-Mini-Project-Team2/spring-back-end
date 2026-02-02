package koscom.mini_project.team2.team2.domain.etf.service;

import jakarta.persistence.EntityNotFoundException;
import koscom.mini_project.team2.team2.domain.etf.dto.ETFCreateRequest;
import koscom.mini_project.team2.team2.domain.etf.dto.EtfResponse;
import koscom.mini_project.team2.team2.domain.etf.entity.Etf;
import koscom.mini_project.team2.team2.domain.etf.repository.EtfRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class EtfService {

    private final EtfRepository etfRepository;

    public EtfService(EtfRepository etfRepository) {
        this.etfRepository = etfRepository;
    }

    public EtfResponse create(ETFCreateRequest request) {
        Etf etf = Etf.builder()
                .name(request.name())
                .fltRt(request.fltRt())
                .riskLevel(request.riskLevel())
                .category(request.category())
                .description(request.description())
                .build();
        Etf saved = etfRepository.save(etf);
        return EtfResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public EtfResponse findById(Long id) {
        Etf etf = etfRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Etf not found. id=" + id));
        return EtfResponse.from(etf);
    }

    @Transactional(readOnly = true)
    public List<EtfResponse> findAll() {
        return etfRepository.findAll().stream()
                .map(EtfResponse::from)
                .toList();
    }

    public void delete(Long id) {
        if (!etfRepository.existsById(id)) {
            throw new EntityNotFoundException("Dummy not found. id=" + id);
        }
        etfRepository.deleteById(id);
    }
}