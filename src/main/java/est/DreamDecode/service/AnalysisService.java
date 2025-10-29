package est.DreamDecode.service;

import est.DreamDecode.domain.Analysis;
import est.DreamDecode.dto.AddAnalysisRequest;
import est.DreamDecode.dto.UpdateAnalysisRequest;
import est.DreamDecode.repository.AnalysisRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final AnalysisRepository analysisRepository;

    public Analysis addAnalysis(AddAnalysisRequest request){
        return analysisRepository.save(request.toEntity());
    }

    public Analysis getAnalysisById(Long analysisId){
        return analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public Analysis updateAnalysis(Long analysisId, UpdateAnalysisRequest request){
        Analysis analysis = analysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        analysis.updateAnalysis(request.getAnalysisResult(), request.getSentiment());
        return analysis;
    }

    public void deleteAnalysisById(Long analysisId){
        analysisRepository.deleteById(analysisId);
    }
}
