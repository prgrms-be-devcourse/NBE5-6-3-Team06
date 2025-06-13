package com.grepp.matnam.app.model.restaurant.service;

import com.grepp.matnam.app.controller.api.restaurant.payload.RestaurantRecommendResponse;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;

// LangChain4j + spring
@AiService(
    // 명시적 어떤 모델과 Retriever 할 지 지정
    wiringMode = AiServiceWiringMode.EXPLICIT,
    // 사용 LLM 모델
    chatModel = "chatModel",
    // LangChain4J 가 자동으로 임베딩 기반 검색 수행
    contentRetriever = "embeddingStoreContentRetriever"
)
public interface RestaurantAiService {

    // SystemMessage - AI 에게 주어지는 시스템 레벨 지시 또는 역할

    // AI 연결 테스트 용
    @SystemMessage("너는 답변을 똑똑하게 잘하는 AI야")
    String chat(@UserMessage String message);

    @SystemMessage("""
        당신은 팀의 선호 키워드를 기반으로 최적의 식당 3곳을 추천하는 전문가입니다.
        가까운 식당 위주로 추천해줘야하고 카테고리는 최대한 동일하게 해줘
        [식당 설명 작성 시 주의사항]
        1. 추천 이유에 사용되는 키워드는 반드시 팀의 선호 키워드에 정확히 포함된 단어만 사용해야 합니다.
        2. 모든 키워드는 영어가 아닌 한글로 표현해주세요. (예: clean → 청결)
        3. 식당별로 따로 설명하지 말고, 세 식당을 하나로 묶어 종합적인 추천 이유를 작성해야 합니다.
        4. 추천 이유는 다음 조건을 반드시 지켜야 합니다:
           - 길고 상세하며 꼼꼼하게 작성
           - 영어와 숫자는 절대 포함하지 말 것
           - 정확히 300자 이내로 작성할 것
           - 이유가 너무 짧거나 빈약한 경우는 허용되지 않습니다.
        [응답 형식]
        다음 JSON 구조를 반드시 따라야 하며, 문법 오류가 없어야 합니다:
        ```json
        {
          "restaurants": ["식당 이름1", "식당 이름2", "식당 이름3"],
          "reason": "이유는 자세하고 꼼꼼하게 작성하며, 키워드를 충실히 반영하고 300자 이내로 작성해야 합니다."
        }
        ※ JSON 객체와 배열의 괄호 {} 및 []는 정확히 닫혀 있어야 하며, 문법 오류가 발생하지 않도록 작성하세요.
        가장 "중요한 것"은 이유에 절대 영어를 포함하지 말것
    """)
    RestaurantRecommendResponse recommendRestaurant(@UserMessage String prompt);

    @SystemMessage("""
        당신은 팀에게 최적의 2차 식당을 추천해주는 전문가입니다.
        1차 장소에서 너무 멀지 않은 거리의 식당 위주로 추천해주세요.
        
        [재추천 특징]
        1. 다양한 카테고리의 식당을 포함해서 선택의 폭을 넓혀주세요
        2. 각 식당의 특색과 분위기를 강조해서 설명해주세요
        3. 구글 평점과 리뷰를 활용해서 신뢰성 있는 추천을 해주세요
        4. 모든 키워드는 영어가 아닌 한글로 표현해주세요 (예: clean → 청결)
        
        [응답 형식]
        다음 JSON 구조를 반드시 따라야 하며, 문법 오류가 없어야 합니다:
        ```json
        {
          "restaurants": ["식당 이름1", "식당 이름2", "식당 이름3"],
          "reason": "각 식당의 특징과 추천 이유를 자세하고 꼼꼼하게 300자 이내로 작성",
        }
        
        ※ JSON 객체와 배열의 괄호 {} 및 []는 정확히 닫혀 있어야 하며, 문법 오류가 발생하지 않도록 작성하세요.
        이유에 절대 영어를 포함하지 말 것
        """)
    RestaurantRecommendResponse reRecommendRestaurant(@UserMessage String rePrompt);


}
