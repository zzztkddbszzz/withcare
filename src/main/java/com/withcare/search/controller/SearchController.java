package com.withcare.search.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.withcare.profile.dto.ProfileDTO;
import com.withcare.search.dto.SearchDTO;
import com.withcare.search.dto.SearchResultDTO;
import com.withcare.search.service.SearchService;
import com.withcare.util.JwtToken.JwtUtils;

@CrossOrigin
@RestController
@RequestMapping("/search")
public class SearchController {

    Logger log = LoggerFactory.getLogger(getClass());

    Map<String, Object> result;

    @Autowired
    private SearchService svc;
    
    
    @PostMapping
    public Map<String, Object> search(
            @RequestBody SearchDTO dto,
            @RequestHeader Map<String, String>header) {
        result = new HashMap<>();
        
        try {
            // 권한 체크 (선택적) - 로그인 여부 확인
            String token = header.get("authorization");
            String loginId = null;
            
            if (token != null && !token.isEmpty()) {
                try {
                    loginId = (String) JwtUtils.readToken(token).get("id");
                    dto.setSch_id(loginId); // 로그인한 경우만 검색어 저장용 ID 설정
                } catch (Exception e) {
                    log.warn("토큰 검증 실패, 비로그인 상태로 처리합니다.");
                }
            }
            
            // 1. 검색 결과 조회 (로그인 여부와 관계없이 수행)
            List<SearchResultDTO> searchResults = svc.getSearchResult(dto);
            int totalCount = svc.getSearchResultCount(dto);  // 전체 검색 결과 수
            int totalPages = (int) Math.ceil((double) totalCount / dto.getPageSize());  // 전체 페이지 수

            result.put("success", true);
            result.put("data", searchResults);
            result.put("totalPages", totalPages);
            result.put("currentPage", dto.getPage());
            result.put("totalCount", totalCount);
            
            // 2. 검색어 저장 (로그인한 경우에만)
            if (loginId != null) {
                try {
                    // 검색어가 비어있거나 null인 경우 저장하지 않음
                    if (dto.getSch_keyword() != null && !dto.getSch_keyword().trim().isEmpty()) {
                        svc.insertSearch(dto);
                        result.put("searchSaved", true);
                    } else {
                        log.warn("빈 검색어 감지: 검색어 저장 건너뜀");
                        result.put("searchSaved", false);
                    }
                } catch (Exception e) {
                    log.error("검색어 저장 중 오류 발생", e);
                    result.put("searchSaved", false);
                }
            } else {
                result.put("searchSaved", false);
                result.put("message", "비로그인 상태로 검색 결과만 제공됩니다.");
            }
            
        } catch (Exception e) {
            log.error("검색 처리 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "검색 처리 중 오류가 발생했습니다.");
        }

        return result;
    }
    
    // 최근 검색어 조회
    @GetMapping("/recent/{sch_id}")
    public Map<String, Object>searchRecent(
    		@PathVariable String sch_id,
    		@RequestHeader Map<String, String>header){
    	result = new HashMap<>();

	    
    	try {
    	    String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
    	    
        	List<SearchDTO> recentList = svc.searchRecent(loginId);
        	result.put("success", true);
        	result.put("data", recentList);
		} catch (Exception e) {
	    	result.put("success", false);
		}

    	return result;
    }
    
    // 추천 게시글 (가장 최근 검색어 1개 기반의 게시글 가져오기)
    @GetMapping("/recommend/{sch_id}")
    public Map<String, Object>recommendPost(
    		@PathVariable String sch_id,
    		@RequestHeader Map<String, String>header){
    	
    	result = new HashMap<>();
    	
    	try {
    	    String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
    	    
    		// 최근 검색어 10개 조회
        	List<SearchResultDTO>recommendPost = svc.recommendPost(loginId);
        	
        	result.put("success", true);
        	result.put("data", recommendPost);
		} catch (Exception e) {
			result.put("success", false);
		}
    	return result;
    }
    
    // 로그인 X or 검색 기록 X
    @GetMapping("/recommend/default")
    public Map<String, Object> recommendDefault(
    		@RequestHeader Map<String, String>header){
    	
    	result = new HashMap<>();
    	
    	try {
            String loginId = null;
            if (header != null && header.containsKey("authorization")) {
                loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
            }

            List<SearchResultDTO> recommendPost;
            if (loginId == null) {
                // 로그인 안 된 상태 → 기본 인기글 추천
                recommendPost = svc.recommendDefault();
            } else {
                // 로그인 O → 최근 검색어 기준 추천글 조회
                boolean hasSearchHistory = svc.searchHistory(loginId);
                if (hasSearchHistory) {
                    recommendPost = svc.recommendPost(loginId);
                    if (recommendPost == null || recommendPost.isEmpty()) {
                        recommendPost = svc.recommendDefault();
                    }
                } else {
                    recommendPost = svc.recommendDefault();
                }
            }
			result.put("success", true);
			result.put("data", recommendPost);
		} catch (Exception e) {
			result.put("success", false);
		}
    	return result;
    }
    
    // 전체 인기 검색어
    @GetMapping("/popular")
    public Map<String, Object>searchPopular(){
    	result = new HashMap<>();
    	try {
			List<Map<String, Object>>keywordList = svc.searchPopular();
			result.put("success", true);
			result.put("data", keywordList);
		} catch (Exception e) {
			result.put("success", false);
		}
    	return result;
    }
    
    // 암 종류 및 병기를 제목 + 내용에 포함하는 게시글 조회
    // 둘 다 포함 > 암 종류만 포함 > 병기만 포함
    @PostMapping("/cancer")
    public Map<String, Object> searchCancer(
            @RequestHeader Map<String, String> header,
            @RequestBody(required = false) Map<String, Object> params) {
        
        result = new HashMap<>();
        log.info("▶▶▶ /search/cancer API 호출됨, 파라미터: {}", params);
        
        try {
            // 토큰에서 로그인 ID 추출
            String loginId = null;
            try {
                loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
                log.info("▶▶▶ 로그인 ID: {}", loginId);
            } catch (Exception e) {
                log.error("▶▶▶ 토큰에서 로그인 ID 추출 실패", e);
                result.put("success", false);
                result.put("message", "로그인 정보가 유효하지 않습니다.");
                return result;
            }
            
            if (loginId == null || loginId.trim().isEmpty()) {
                log.warn("▶▶▶ 로그인 ID가 없습니다. 기본 추천 게시글을 반환합니다.");
                result.put("success", true);
                result.put("data", svc.recommendDefault());
                return result;
            }
            
            // 로그인 사용자 프로필 정보 가져오기
            ProfileDTO profileDTO = svc.profileCancer(loginId);
            
            // 프로필 정보가 없는 경우 기본 추천 게시글 반환
            if (profileDTO == null) {
                log.warn("▶▶▶ 프로필 정보가 없습니다. 기본 추천 게시글을 반환합니다.");
                result.put("success", true);
                result.put("data", svc.recommendDefault());
                return result;
            }
            
            profileDTO.setId(loginId);
            
            // 암 종류 정보 확인
            boolean hasCancerInfo = false;
            if (profileDTO.getCancer_idx() != null && profileDTO.getCancer_idx() > 0 && profileDTO.getCancer_name() != null) {
                hasCancerInfo = true;
                log.info("▶▶▶ 암 종류 정보: cancer_idx={}, cancer_name={}", profileDTO.getCancer_idx(), profileDTO.getCancer_name());
            } else {
                log.info("▶▶▶ 암 종류 정보 없음");
            }

            boolean hasStageInfo = false;
            if (profileDTO.getStage_idx() != null && profileDTO.getStage_idx() > 0 && profileDTO.getStage_name() != null) {
                hasStageInfo = true;
                log.info("▶▶▶ 병기 정보: stage_idx={}, stage_name={}", profileDTO.getStage_idx(), profileDTO.getStage_name());
            } else {
                log.info("▶▶▶ 병기 정보 없음");
            }
            
            log.info("▶▶▶ 암 종류 정보 있음: {}, 병기 정보 있음: {}", hasCancerInfo, hasStageInfo);
            
            // 암 종류나 병기 정보가 하나도 없는 경우 기본 추천 게시글 반환
            if (!hasCancerInfo && !hasStageInfo) {
                log.warn("▶▶▶ 암 종류와 병기 정보가 모두 없습니다. 기본 추천 게시글을 반환합니다.");
                result.put("success", true);
                result.put("data", svc.recommendDefault());
                return result;
            }
            
            // 암 관련 게시글 검색
            log.info("▶▶▶ 암 관련 게시글 검색 시작");
            List<SearchResultDTO> results = svc.searchCancer(profileDTO);
            log.info("▶▶▶ 검색 결과 수: {}", results != null ? results.size() : 0);
            
            if (results != null && !results.isEmpty()) {
                log.info("▶▶▶ 검색 결과 첫 번째 게시글: board_idx={}, title={}",
                        results.get(0).getBoard_idx(), results.get(0).getTitle());
                
                if (results.get(0).getLike_count() != null) {
                    log.info("▶▶▶ 첫 번째 게시글 추천 수: {}", results.get(0).getLike_count());
                } else {
                    log.info("▶▶▶ 첫 번째 게시글 추천 수: null");
                }
            }
            
            // 검색 결과가 없는 경우 기본 추천 게시글 반환
            if (results == null || results.isEmpty()) {
                log.warn("▶▶▶ 검색 결과가 없습니다. 기본 추천 게시글을 반환합니다.");
                result.put("success", true);
                result.put("data", svc.recommendDefault());
                return result;
            }
            
            log.info("▶▶▶ 암 관련 게시글 검색 완료, 결과 반환");
            result.put("success", true);
            result.put("data", results);
        } catch (Exception e) {
            log.error("▶▶▶ 암 관련 게시글 검색 중 오류 발생", e);
            result.put("success", false);
            result.put("message", "로그인 정보가 유효하지 않습니다.");
        }

        return result;
    }
    
}