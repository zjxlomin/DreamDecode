package est.DreamDecode.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import est.DreamDecode.domain.Analysis;
import est.DreamDecode.domain.Dream;
import est.DreamDecode.domain.Scene;
import est.DreamDecode.dto.AnalysisResponse;
import est.DreamDecode.dto.SentimentResult;
import est.DreamDecode.repository.AnalysisRepository;
import est.DreamDecode.repository.DreamRepository;
import est.DreamDecode.repository.SceneRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalysisService {
    private final AnalysisRepository analysisRepository;
    private final DreamRepository dreamRepository;
    private final SceneRepository sceneRepository;
    private final NaturalLanguageService naturalLanguageService;

    @Transactional
    public AnalysisResponse addOrUpdateAnalysis(Long dreamId, boolean isPost){
        Dream dream = dreamRepository.findById(dreamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if(!isPost) {
            sceneRepository.deleteByDreamId(dreamId); // 기존 장면 삭제 후 추가
        }

        String dreamContent = dream.getContent();
        JSONObject dreamAnalysis = dreamAnalyzeByPython(dreamContent);

        List<Object> scenes = dreamAnalysis.getJSONArray("analysis").toList();
        for(Object s : scenes){
            ObjectMapper mapper = new ObjectMapper();
            Map<String, String> map = mapper.convertValue(s, Map.class);
            String content = map.get("scene");
            String emotion = map.get("emotion");
            String interpretation = map.get("interpretation");
            Scene scene = new Scene();
            scene.setContent(content);
            scene.setEmotion(emotion);
            scene.setInterpretation(interpretation);
            scene.setDream(dream);
            sceneRepository.save(scene);
        }
        String insight = dreamAnalysis.getString("insight");
        String suggestion = dreamAnalysis.getString("suggestion");
        
        // JSON 배열을 쉼표로 구분된 문자열로 변환 (괄호 제거)
        List<Object> categoriesList = dreamAnalysis.getJSONArray("categories").toList();
        String categories = categoriesList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        
        List<Object> tagsList = dreamAnalysis.getJSONArray("tags").toList();
        String tags = tagsList.stream()
                .map(Object::toString)
                .collect(Collectors.joining(","));
        
        String summary = dreamAnalysis.getString("summary");
        
        // GCP Natural Language API를 사용하여 꿈 내용의 실제 감정 분석 수행
        SentimentResult sentimentResult = naturalLanguageService.analyzeSentiment(summary);
        double sentiment = sentimentResult.getScore();
        double magnitude = sentimentResult.getMagnitude();

        /* categories, tags List<String>으로 변환 후
        try{
            ObjectMapper mapper = new ObjectMapper();
            List<String> categoriesToList = mapper.readValue(categories, new TypeReference<List<String>>() {});
            List<String> tagsToList = mapper.readValue(tags, new TypeReference<List<String>>() {});
            for(String c : categoriesToList){
                System.out.println(c);
            }
            for(String t : tagsToList){
                System.out.println(t);
            }
        } catch(JsonProcessingException e) {

        }
        */

        if(isPost) {
            Analysis analysis = new Analysis();
            analysis.setInsight(insight);
            analysis.setSuggestion(suggestion);
            analysis.setSummary(summary);
            analysis.setSentiment(sentiment);
            analysis.setMagnitude(magnitude);
            analysis.setDream(dream);
            dream.updateCatAndTags(categories, tags);
            return new AnalysisResponse(analysisRepository.save(analysis));
        }
        else {
            Analysis analysis = getAnalysisByDreamId(dreamId);
            analysis.updateAnalysis(
                    insight,
                    suggestion,
                    summary,
                    sentiment,
                    magnitude
            );
            dream.updateCatAndTags(categories, tags);
            return new AnalysisResponse(analysis);
        }

    }

    public Analysis getAnalysisByDreamId(Long dreamId){
        return analysisRepository.findAll()
                .stream().filter(a -> a.getDream().getId().equals(dreamId))
                .toList().get(0);
    }

    // 프롬프트
    public JSONObject dreamAnalyzeByPython(String dreamContent){
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
                
                마지막으로, 꿈 전반에 걸쳐 느껴지는 정서를 자연스럽게 풀어서 표현한 100자 미만의 문장으로 작성하세요.
                
                반드시 아래 JSON 형식의 구조를 지키면서 응답하세요.
                JSON 키 이름은 analysis, scene, emotion, interpretation, insight, suggestion, categories, tags, summary로 반드시 유지하고, 다른 키는 추가하지 마세요.
                
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
                    "insight": "꿈 전체의 심리적 해석과 감정 경향 요약",
                    "suggestion": "감정을 다스리거나 회복하기 위한 조언",
                    "categories": ["주요 주제 카테고리들"],
                    "tags": ["연관 태그들"],
                    "summary": "꿈 전반에 걸쳐 느껴지는 정서를 자연스럽게 풀어서 표현한 100자 미만의 문장"
                }}
                
                꿈의 내용은 다음과 같습니다:
                """
                + dreamContent;

        JSONObject result = singleAlanChat(clientId, prompt);
        resetAlanState(clientId);
        return result;
    }

    private JSONObject singleAlanChat(String clientId, String prompt){
        String url = "https://kdt-api-function.azurewebsites.net/api/v1/question";
        RestTemplate restTemplate = new RestTemplate();

        String requestUrl = url + "?client_id=" + clientId + "&content=" + prompt;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
                requestUrl,
                HttpMethod.GET,
                entity,
                String.class
        );

        try{
            JSONObject jsonResponse = new JSONObject(response.getBody());
            String content = jsonResponse.getString("content");
            content = content.replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();
            return new JSONObject(content);
        } catch(JSONException e){
            return null;
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
