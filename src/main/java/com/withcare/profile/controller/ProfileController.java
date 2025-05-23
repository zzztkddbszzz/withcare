package com.withcare.profile.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.withcare.comment.dto.ComDTO;
import com.withcare.comment.dto.MenDTO;
import com.withcare.post.dto.LikeDislikeDTO;
import com.withcare.post.dto.PostDTO;
import com.withcare.profile.dto.ProfileDTO;
import com.withcare.profile.service.ProfileService;
import com.withcare.search.dto.SearchDTO;
import com.withcare.util.JwtToken.JwtUtils;

@CrossOrigin
@RestController
public class ProfileController {

	@Autowired
	ProfileService svc;

	Map<String, Object> result = null;

	Logger log = LoggerFactory.getLogger(getClass());

	/*
	 * // 프로필 저장 post
	 * 
	 * @PostMapping("/profile") public Map<String, Object> saveProfile(@RequestBody
	 * Map<String, Object> pro) { result = new HashMap<String, Object>();
	 * 
	 * try { boolean success = svc.saveProfile(pro); result.put("success", success);
	 * result.put("message", success ? "프로필 저장 완료" : "프로필 저장 실패"); } catch
	 * (Exception e) { log.error("[오류] 프로필 저장 중 예외 발생", e); result.put("success",
	 * false); result.put("message", "서버 오류"); } return result; }
	 */

	// 프로필 열람 get
	@GetMapping("/profile/{id}")
	public Map<String, Object> getProfile(@PathVariable("id") String id, @RequestHeader Map<String, String> header) {

		Map<String, Object> result = new HashMap<>();

		try {
			String token = header.get("authorization");
			Map<String, Object> payload = JwtUtils.readToken(token);
			String loginId = (String) payload.get("id");

			if (loginId != null && loginId.equals(id)) {
				ProfileDTO dto = svc.getProfile(id);
				result.put("status", "success");
				result.put("data", dto);
			} else {
				result.put("status", "fail");
				result.put("message", "인증된 사용자만 접근할 수 있습니다.");
			}

		} catch (Exception e) {
			result.put("status", "error");
			result.put("message", "서버 오류: " + e.getMessage());
		}

		return result;
	}

	// 회원 개인 정보 수정 기능 put
	@PutMapping("/profile/update")
	public Map<String, Object> updateProfile(@RequestBody ProfileDTO dto,
			@RequestHeader("Authorization") String token) {

		Map<String, Object> result = new HashMap<>();

		try {
			// 1. 토큰 파싱 → 사용자 ID 추출
			Map<String, Object> payload = JwtUtils.readToken(token);
			String tokenId = (String) payload.get("id");

			// 2. 사용자 ID로 프로필 정보 갱신
			dto.setId(tokenId);
			int updated = svc.updateProfile(dto);

			result.put("status", updated > 0 ? "success" : "fail");

		} catch (Exception e) {
			result.put("status", "error");
			result.put("message", e.getMessage());
		}

		return result;
	}

	// 타인이 프로필 확인하는 기능 get
	@GetMapping("/profile/view/{id}")
	public Map<String, Object> viewOtherProfile(
	        @PathVariable("id") String id,
	        @RequestHeader(value = "authorization", required = false) String token) {

	    Map<String, Object> result = new HashMap<>();

	    try {
	        // 1. 토큰 없으면 로그인 필요
	        if (token == null || token.trim().isEmpty()) {
	            result.put("status", "fail");
	            result.put("message", "로그인이 필요합니다.");
	            return result;
	        }

	        // 2. 토큰 유효성 검사 및 ID 추출
	        Map<String, Object> payload = JwtUtils.readToken(token);
	        if (payload == null || !payload.containsKey("id") || payload.get("id") == null) {
	            result.put("status", "fail");
	            result.put("message", "유효하지 않은 토큰입니다.");
	            return result;
	        }

	        // 3. 프로필 기본 정보 조회
	        ProfileDTO profile = svc.getProfileById(id);

	        // 4. 활동 정보 조회
	        List<PostDTO> posts = svc.getUserPosts(id);
	        List<ComDTO> comments = svc.getUserComments(id);
	        List<LikeDislikeDTO> likes = svc.getUserLikes(id);
	        List<SearchDTO> searches = svc.getUserSearches(id);
	        List<MenDTO> mentions = svc.getUserMentions(id);

	        // 5. 응답 조립
	        result.put("status", "success");
	        result.put("profile", profile);
	        result.put("posts", posts);
	        result.put("comments", comments);
	        result.put("likes", likes);
	        result.put("searches", searches);
	        result.put("mentions", mentions);

	    } catch (Exception e) {
	        result.put("status", "error");
	        result.put("message", "서버 오류: " + e.getMessage());
	    }

	    return result;
	}

	@GetMapping("/profile/activity/{id}")
	public Map<String, Object> getUserActivity(@PathVariable("id") String id,
			@RequestHeader Map<String, String> header) {

		Map<String, Object> result = new HashMap<>();

		try {
			// 1. 토큰 추출 및 검증
			String token = header.get("authorization");
			if (token == null || token.trim().isEmpty()) {
				result.put("status", "fail");
				result.put("message", "로그인이 필요합니다.");
				return result;
			}

			// 2. 토큰 유효성 검사만 (사용자 ID 비교는 하지 않음)
			JwtUtils.readToken(token); // 예외 발생 시 catch로 이동

			// 3. 해당 사용자 활동 정보 조회
			List<PostDTO> posts = svc.getUserPosts(id);
			List<ComDTO> comments = svc.getUserComments(id);
			List<LikeDislikeDTO> likes = svc.getUserLikes(id);
			List<SearchDTO> searches = svc.getUserSearches(id);
			List<MenDTO> mentions = svc.getUserMentions(id);

			// 4. 응답 조립
			result.put("status", "success");
			result.put("posts", posts);
			result.put("comments", comments);
			result.put("likes", likes);
			result.put("searches", searches);
			result.put("mentions", mentions);

		} catch (Exception e) {
			result.put("status", "error");
			result.put("message", "서버 오류: " + e.getMessage());
		}

		return result;
	}

}
