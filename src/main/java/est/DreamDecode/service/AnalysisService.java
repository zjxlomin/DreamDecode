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

        String prompt = """
                당신은 사용자의 꿈을 심리적으로 해석하고, 감정적 의미와 통찰을 제시하는 역할을 맡고 있습니다.
                아래의 지침에 따라 꿈 내용을 분석해 주세요.
                
                꿈에서 등장한 주요 장면을 2~4개로 나누고, 각 장면마다 느껴진 감정과 그 감정이 나타난 이유(심리적 의미)를 분석하세요.
                
                꿈 전체를 관통하는 심리적 흐름이나 무의식적인 메시지를 설명하세요.
                
                꿈의 감정을 긍정적으로 다스리거나 회복하기 위한 현실적인 조언을 제시하세요.
                
                꿈의 내용을 대표할 수 있는 주제를 하나 이상의 카테고리로 분류하세요.
                (예: 불안 / 성장 / 관계 / 도전 / 상실 / 자아 / 자유 / 변화 / 사랑 / 기억 / 두려움 등)
                
                꿈의 주요 키워드를 기반으로 3~5개의 태그를 생성하세요.
                
                반드시 아래 JSON 형식으로 응답하세요.
                JSON 키 이름은 analysis, scene, emotion, interpretation, overall_insight, suggestion, category, tags로 반드시 유지하고, 다른 키는 추가하지 마세요.
                각 항목의 값은 자연스러운 문장으로 작성하되, 구조는 반드시 유지하세요.
                
                {{
                    "analysis": [
                        {{
                            "scene": "장면1 요약",
                            "emotion": "주된 감정1",
                            "interpretation": "이 감정이 나타난 이유나 의미"
                        }},
                        {{
                            "scene": "장면2 요약",
                            "emotion": "주된 감정2",
                            "interpretation": "이 감정이 나타난 이유나 의미"
                        }},
                        ...
                    ],
                    "overall_insight": "꿈 전체의 심리적 해석과 감정 경향 요약",
                    "suggestion": "감정을 다스리거나 회복하기 위한 조언",
                    "category": ["주요 주제 카테고리들"],
                    "tags": ["연관 태그들"]
                }}
                
                꿈의 내용은 다음과 같습니다:
                """ + dreamContent;
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
