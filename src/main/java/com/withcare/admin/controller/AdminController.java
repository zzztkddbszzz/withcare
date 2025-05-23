package com.withcare.admin.controller;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.withcare.admin.dto.AdminMemberDTO;
import com.withcare.admin.dto.AdminMemberDetailDTO;
import com.withcare.admin.service.AdminService;
import com.withcare.member.dto.MemberDTO;
import com.withcare.post.dto.LikeDislikeDTO;
import com.withcare.post.dto.PostDTO;
import com.withcare.profile.dto.BadgeDTO;
import com.withcare.profile.dto.LevelDTO;
import com.withcare.profile.dto.TimelineDTO;
import com.withcare.util.JwtToken.JwtUtils;

@CrossOrigin
@RestController
public class AdminController {
	
	@Autowired AdminService svc;
	
	Logger log = LoggerFactory.getLogger(getClass());
	
	Map<String, Object> result = null;
	
    @Value("${file.upload-dir}")
    private String baseUploadPath;

    @Value("${upload.url-prefix}")
    private String urlPrefix;
	
	// 관리자 권한 부여
	@PutMapping("/admin/grant")
	public Map<String, Object>adminGrant(
			@RequestBody MemberDTO memberDTO,
			@RequestHeader Map<String, String>header){
		
		result = new HashMap<String, Object>();
		Map<String, Object> params = new HashMap<>();
		
		String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
		
		boolean login = false;
		boolean success = false;
		
		// 로그인 사용자 레벨 확인
		int user_lv = svc.userLevel(loginId);
		
		// 관리자 레벨 체크
		if (user_lv != 7) {
			result.put("success", false);
			return result;
		}
		
		// 로그인 유효성 체크
		if (loginId != null && !loginId.isEmpty()) {
			login = true;
			
			// 권한 부여 실행
			params.put("id", memberDTO.getId());
			params.put("lv_idx", memberDTO.getLv_idx());
			
			success = svc.levelUpdate(params);
		}
		
		// 결과 반환
		result.put("success", success);
		result.put("loginYN", login);
		
		return result;
	}
	
	// 멤버 리스트 확인
	@PostMapping("/admin/member/list")
	public Map<String, Object> adminMemberList(
	        @RequestBody(required = false) Map<String, Object> params
	        ,@RequestHeader Map<String, String> header) {
		
		result = new HashMap<String, Object>();
		
		String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
		
		boolean login = false;
		
		// 로그인 유효성 체크
		if (loginId == null || loginId.isEmpty()) {
			result.put("success", false);
			login = true;
			return result;
		}
		
		// 로그인 사용자 레벨 확인
		int user_lv = svc.userLevel(loginId);
		
		// 관리자 레벨 체크
		if (user_lv != 7) {
			result.put("success", false);
			return result;
		}
		
		// null 이면 가져오지 말아라
	    String searchId = (params != null) ? (String) params.get("searchId") : null;
		String sortField = (params != null) ? (String) params.get("sortField") : null;
		String sortOrder= (params != null) ? (String) params.get("sortOrder") : null;
		String blockFilter = (params != null) ? (String) params.get("blockFilter") : null;
		
		// page, size 초기화
	    int page = 1;
	    int size = 10;
		
	    // null 아닐때만 가져와라
        if (params != null && params.get("page") != null) {
            page = Integer.parseInt(params.get("page").toString());
        }
	    
        if (params != null && params.get("size") != null) {
            size = Integer.parseInt(params.get("size").toString());
        }
        
        // start 초기화
		int start = (page - 1) * size;
		
		result.put("searchId", searchId);
		result.put("start", start);
		result.put("size", size);
		result.put("sortField", sortField);
		result.put("sortOrder", sortOrder);
		result.put("blockFilter", blockFilter);
		
		List<AdminMemberDTO> memberList = svc.adminMemberList(result);
		
		result.put("success", true);
		result.put("data", memberList);
		result.put("loginId", loginId);
		
	    return result;
	}
	
	// 회원 정보 상세보기
	@PostMapping("/admin/member/detail")
	public Map<String, Object> adminMemberDetail(
			@RequestBody Map<String, String> param,
			@RequestHeader Map<String, String>header){
		
		Map<String, Object> result = new HashMap<String, Object>();
		
		String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
		
		if (loginId == null || loginId.isEmpty() || svc.userLevel(loginId)!=7) {
			result.put("success", false);
			return result;
		}
		
		String targetId = param.get("id");
		
		AdminMemberDetailDTO detail = svc.adminMemberDetail(targetId);
		
		result.put("success", true);
		result.put("data", detail);
		return result;
	}
	
	
	// 작성 게시글 목록 확인
	@PostMapping("/admin/member/post")
	public Map<String, Object> adminMemberPost(
			@RequestBody Map<String, String>param,
			@RequestHeader Map<String, String>header){
		
		Map<String, Object> result = new HashMap<String, Object>();
		String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
		
		if (loginId == null || loginId.isEmpty() || svc.userLevel(loginId)!=7) {
			result.put("success", false);
			return result;
		}
		
		String targetId = param.get("id");
		List<PostDTO> posts = svc.adminMemberPost(targetId);
		
		result.put("success", true);
		result.put("data", posts);
		
		return result;
	}
	
	// 댓글 + 멘션 목록 조회
	@PostMapping("/admin/member/comments")
	public Map<String, Object> adminMemberComments(
			@RequestBody Map<String, String>param,
			@RequestHeader Map<String, String>header){
		
		result = new HashMap<String, Object>();
		String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
		
		if (loginId == null || loginId.isEmpty() || svc.userLevel(loginId)!=7) {
			result.put("success", false);
			return result;
		}
		
		String targetId = param.get("id");
		
		List<Map<String, Object>> commentList = svc.adminMemberComments(targetId);
		
		result.put("success", true);
		result.put("data", commentList);
		
		return result;
	}
	
	// 추천 누른 게시글 (비추천 X)
	@PostMapping("/admin/member/like")
	public Map<String, Object>adminMemberLike(
			@RequestBody Map<String, String>param,
			@RequestHeader Map<String, String>header){
		
		result = new HashMap<String, Object>();
		String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
		
		if (loginId == null || loginId.isEmpty() || svc.userLevel(loginId)!=7) {
			result.put("success", false);
			return result;
		}
		
		String targetId = param.get("id");
		
		List<LikeDislikeDTO> likes = svc.adminMemberLike(targetId);
		result.put("success", true);
		result.put("data", likes);
		
		return result;
	}
	
	// 타임라인 확인
	@PostMapping("/admin/member/timelines")
	public Map<String, Object>adminMemberTimeline(
			@RequestBody Map<String, String>param,
			@RequestHeader Map<String, String>header){
		
		result = new HashMap<String, Object>();
		String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
		
		if (loginId == null || loginId.isEmpty() || svc.userLevel(loginId)!=7) {
			result.put("success", false);
			return result;
		}
		
		String targetId = param.get("id");
		
		List<TimelineDTO> timeline = svc.adminMemberTimeline(targetId);
		result.put("success", true);
		result.put("data", timeline);
		
		return result;
	}
	
    // 공통 파일 저장 함수
    private String saveFile(MultipartFile file, String type) throws Exception {
        String original = file.getOriginalFilename();
        if (original == null || !original.matches(".*\\.(png|jpg|jpeg|webp)$")) {
            throw new IllegalArgumentException("지원하지 않는 이미지 형식입니다.");
        }
        String ext = original.substring(original.lastIndexOf("."));
        String fileName = UUID.randomUUID() + ext;
        Path saveDir = Paths.get(baseUploadPath, type);
        Files.createDirectories(saveDir);
        Path savePath = saveDir.resolve(fileName);
        Files.write(savePath, file.getBytes());
        return urlPrefix + "/" + type + "/" + fileName;
    }

    // 배지 통합 등록/수정
    @PostMapping("/admin/bdg/save")
    public Map<String, Object> saveBadge(
            @RequestParam(value = "bdg_idx", required = false) Integer bdgIdx,
            @RequestParam("file") MultipartFile file,
            @RequestParam("bdg_name") String bdgName,
            @RequestParam("bdg_condition") String bdgCondition,
            @RequestParam("bdg_active_yn") boolean bdgActiveYn,
            @RequestHeader Map<String, String> header) {

        result = new HashMap<>();
        String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
        if (loginId == null || loginId.isEmpty() || svc.userLevel(loginId) != 7) {
            result.put("success", false);
            result.put("msg", "관리자 권한이 필요합니다.");
            return result;
        }
        try {
            String url = saveFile(file, "badge");
            BadgeDTO dto = new BadgeDTO();
            dto.setBdg_name(bdgName);
            dto.setBdg_icon(url);
            dto.setBdg_condition(bdgCondition);
            dto.setBdg_active_yn(bdgActiveYn);
            boolean success;
            if (bdgIdx != null) {
                dto.setBdg_idx(bdgIdx);
                success = svc.adminBdgUpdate(dto);
            } else {
                success = svc.adminBdgAdd(dto);
            }
            result.put("success", success);
            result.put("url", url);
        } catch (Exception e) {
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    // 배지 삭제
    @PutMapping("/admin/bdg/delete")
    public Map<String, Object> deleteBadge(
    		@RequestBody Map<String, Object> param,
            @RequestHeader Map<String, String> header) {
        int bdgIdx = (int) param.get("bdg_idx");
        result = new HashMap<>();
        
        String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
        
        if (loginId == null || loginId.isEmpty() || svc.userLevel(loginId) != 7) {
            result.put("success", false);
            result.put("msg", "관리자 권한이 필요합니다.");
            return result;
        }
        
        BadgeDTO dto = new BadgeDTO();
        dto.setBdg_idx(bdgIdx);
        dto.setBdg_active_yn(false);
        
        boolean success = svc.adminBdgDelete(dto);
        result.put("success", success);
        return result;
    }

    // 레벨 통합 등록/수정
    @PostMapping("/admin/level/save")
    public Map<String, Object> saveLevel(
            @RequestParam(value = "lv_idx", required = false) Integer lvIdx,
            @RequestParam("file") MultipartFile file,
            @RequestParam("lv_no") int lvNo,
            @RequestParam("lv_name") String lvName,
            @RequestParam("post_cnt") int postCnt,
            @RequestParam("com_cnt") int comCnt,
            @RequestParam("like_cnt") int likeCnt,
            @RequestParam("time_cnt") int timeCnt,
            @RequestParam("access_cnt") int accessCnt,
            @RequestHeader Map<String, String> header) {

        result = new HashMap<>();
        String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");
        if (loginId == null || loginId.isEmpty() || svc.userLevel(loginId) != 7) {
            result.put("success", false);
            result.put("msg", "관리자 권한이 필요합니다.");
            return result;
        }
        try {
            String url = saveFile(file, "level");
            LevelDTO dto = new LevelDTO();
            dto.setLv_no(lvNo);
            dto.setLv_name(lvName);
            dto.setLv_icon(url);
            dto.setPost_cnt(postCnt);
            dto.setCom_cnt(comCnt);
            dto.setLike_cnt(likeCnt);
            dto.setTime_cnt(timeCnt);
            dto.setAccess_cnt(accessCnt);
            boolean success;
            if (lvIdx != null) {
                dto.setLv_idx(lvIdx);
                success = svc.adminLevelUpdate(dto);
            } else {
                success = svc.adminLevelAdd(dto);
            }
            result.put("success", success);
            result.put("url", url);
        } catch (Exception e) {
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }
    
    
    // 이미지 삭제 (관리자만 가능)
    @DeleteMapping("/admin/img/delete")
    public Map<String, Object> deleteImage(
    		@RequestParam("url") String imageUrl,
            @RequestHeader Map<String, String> header) {
    	
        Map<String, Object> result = new HashMap<>();
        String loginId = (String) JwtUtils.readToken(header.get("authorization")).get("id");

        if (loginId == null || loginId.isEmpty() || svc.userLevel(loginId) != 7) {
            result.put("success", false);
            result.put("msg", "관리자 권한이 필요합니다.");
            return result;
        }

        try {
            if (!imageUrl.startsWith(urlPrefix)) {
                throw new IllegalArgumentException("삭제할 수 없는 경로입니다.");
            }

            String relativePath = imageUrl.replace(urlPrefix + "/", "");
            Path filePath = Paths.get(baseUploadPath, relativePath);
            Files.deleteIfExists(filePath);

            result.put("success", true);
        } catch (Exception e) {
            result.put("success", false);
            result.put("msg", e.getMessage());
        }

        return result;
    }
}
