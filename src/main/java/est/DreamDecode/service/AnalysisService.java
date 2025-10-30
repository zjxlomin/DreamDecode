package est.DreamDecode.service;

import est.DreamDecode.domain.Analysis;
import est.DreamDecode.domain.Dream;
import est.DreamDecode.dto.AnalysisResponse;
import est.DreamDecode.repository.AnalysisRepository;
import est.DreamDecode.repository.DreamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.python.core.PyFunction;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.util.PythonInterpreter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final AnalysisRepository analysisRepository;
    private final DreamRepository dreamRepository;
    private static PythonInterpreter interpreter;

    public AnalysisResponse addAnalysis(Long dreamId){
        Dream dream = dreamRepository.findById(dreamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String dreamContent = dream.getContent();
        String dreamAnalysis = dreamAnalyzeByPython(dreamContent);
        double sentiment = 1.0;

        Analysis analysis = new Analysis();
        analysis.setAnalysisResult(dreamAnalysis);
        analysis.setSentiment(sentiment);
        analysis.setDream(dream);
        return new AnalysisResponse(analysisRepository.save(analysis));
    }

    public Analysis getAnalysisByDreamId(Long dreamId){
        return analysisRepository.findAll()
                .stream().filter(a -> a.getDream().getId().equals(dreamId))
                .toList().get(0);
    }

    @Transactional
    public Analysis updateAnalysis(Long dreamId){
        Dream dream = dreamRepository.findById(dreamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        String dreamContent = dream.getContent();
        String dreamAnalysis = dreamAnalyzeByPython(dreamContent);
        double sentiment = 1.0;

        Analysis analysis = getAnalysisByDreamId(dreamId);
        analysis.updateAnalysis(dreamAnalysis, sentiment);

        return analysis;
    }

    public void deleteAnalysisById(Long analysisId){
        analysisRepository.deleteById(analysisId);
    }

    public String dreamAnalyzeByPython(String dreamContent){
        interpreter = new PythonInterpreter();
        interpreter.execfile("src/main/resources/static/python/prompt.py"); // test.py 경로

        PyFunction pyFunction = (PyFunction) interpreter.get("testFunc", PyFunction.class);
        PyObject pyobj = pyFunction.__call__(new PyString(dreamContent));

        return pyobj.toString();
    }
}
