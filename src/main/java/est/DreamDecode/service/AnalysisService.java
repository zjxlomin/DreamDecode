package est.DreamDecode.service;

import est.DreamDecode.domain.Analysis;
import est.DreamDecode.domain.Dream;
import est.DreamDecode.dto.AnalysisResponse;
import est.DreamDecode.repository.AnalysisRepository;
import est.DreamDecode.repository.DreamRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final AnalysisRepository analysisRepository;
    private final DreamRepository dreamRepository;

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
        double sentiment = 1.0; // TODO: 감정 점수 산출

        Analysis analysis = getAnalysisByDreamId(dreamId);
        analysis.updateAnalysis(dreamAnalysis, sentiment);

        return analysis;
    }

    public void deleteAnalysisById(Long analysisId){
        analysisRepository.deleteById(analysisId);
    }

    public String dreamAnalyzeByPython(String dreamContent){
        String clientId = "515d3756-783e-484d-a04b-b7121c99fbb7";

        String prompt =
                "이 꿈의 원인이 되는 감정이나 이 꿈이 갖는 상징을 출처 없이 분석해줘: " // TODO: 조건이나 길이 제한 추가.. JSON 형식으로?
                + dreamContent;
        String result = singleAlanChat(clientId, prompt);
        resetAlanState(clientId);
        return result;
    }

    private String singleAlanChat(String clientId, String content){
        String url = "https://kdt-api-function.azurewebsites.net/api/v1/question";
        RestTemplate restTemplate = new RestTemplate();

        String requestUrl = url + "?client_id=" + clientId + "&content=" + content;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                requestUrl,
                HttpMethod.GET,
                entity,
                String.class
        );

        // 응답 JSON 파싱
        try{
            JSONObject jsonResponse = new JSONObject(response.getBody());
            return jsonResponse.getString("content");
        } catch(JSONException e){
            return "Analyze failed";
        }

    }

    private void resetAlanState(String clientId){
        String url = "https://kdt-api-function.azurewebsites.net/api/v1/reset-state";
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        JSONObject params = new JSONObject();
        try{
            params.put("client_id", clientId);
        } catch(JSONException e){

        }
        HttpEntity<String> entity = new HttpEntity<>(params.toString(), headers);

        restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);
    }
}
