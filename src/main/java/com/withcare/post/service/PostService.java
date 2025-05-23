package com.withcare.post.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.withcare.board.dao.BoardDAO;
import com.withcare.post.dao.PostDAO;
import com.withcare.post.dto.LikeDislikeDTO;
import com.withcare.post.dto.PostDTO;

@Service
public class PostService {

	Logger log = LoggerFactory.getLogger(getClass());
	
	HashMap<String, Object> result = null;
	
	private int post_count = 5; // 페이지 당 게시글 수
	
	@Autowired PostDAO dao;
	@Autowired BoardDAO boardDao;
	
	@Value("${file.upload-dir}")
	private String uploadDir; // 우선 user.home 에 있는 uploads 폴더로 경로 지정해뒀습니다. (서혜 언니는 다른 경로 지정 필수)
	
	public boolean postWrite(PostDTO dto) {
	    // 게시판 com_yn 가져오기
	    boolean boardComYn = boardDao.boardComYn(dto.getBoard_idx());
	    dto.setCom_yn(boardComYn);
	    int row = dao.postWrite(dto);
	    return row > 0;
	}

	@Transactional // 파일 하나가 실패하면 모두 롤백되어야 함. (DB 부분)
	public boolean saveFiles(int post_idx, MultipartFile[] files) {
		
    	if (files == null || files.length == 0) {
    	    return true; // 업로드할 파일이 없으면 true 반환
    	}
    	
    	List<String> savedFileNames = new ArrayList<>();
        try {
	        for (MultipartFile file : files) {

	        	if (file.isEmpty()) continue; // file 이 없어도 에러 안나게 해놓은 거
	        	
	            if (file.getSize() > 10 * 1024 * 1024) { // 10MB 제한
	                throw new IllegalArgumentException("파일 사이즈 초과");
	            }
	            
	            // MIME 타입 검사
	            if (!file.getContentType().startsWith("image/")) {
	                throw new IllegalArgumentException("이미지 파일만 업로드 가능합니다.");
	            }
	
	            // 확장자 검사 (jpg, jpeg, png만 허용)
	            String origin_name = file.getOriginalFilename();
	            if (origin_name == null || origin_name.lastIndexOf(".") == -1) {
	                throw new IllegalArgumentException("파일 이름이 잘못되었습니다.");
	            }
	
	            String ext = origin_name.substring(origin_name.lastIndexOf(".") + 1).toLowerCase(); // 작성자가 대문자로 넣으면 그거 변환
	            if (!ext.equals("jpg") && !ext.equals("jpeg") && !ext.equals("png")) {
	                throw new IllegalArgumentException("jpg, jpeg, png 확장자만 업로드 가능합니다.");
	            }
	
	            String extension = origin_name.substring(origin_name.lastIndexOf(".")); // 확장자 "." 에서 자르기
	            
	            // UUID로 파일명 생성
	            String savedName = UUID.randomUUID().toString() + extension;
	            
	                // 파일 저장 경로 생성
	                Path path = Paths.get(uploadDir, savedName);
	
	                // 폴더가 없으면 생성
	                Files.createDirectories(path.getParent());
	
	                // 실제 파일 저장
	                file.transferTo(path.toFile());
	
	                // DB에 파일 URL 저장
	                Map<String, Object> param = new HashMap<>();
	                param.put("post_idx", post_idx);
	                param.put("file_url", savedName);  // 혹은 full path로 저장하고 싶으면 변경
	
	                dao.fileInsert(param);
	                savedFileNames.add(savedName); // 잠재적 롤백을 위해 목록에 추가
	                
	            } 
	        	return true; // 모두 성공했을 경우
	        	
        }catch (Exception e) {
	                e.printStackTrace();

	        // 롤백: 일부가 실패할 경우 성공적으로 저장된 파일을 파일 시스템 및 DB에서 삭제
            for (String savedName : savedFileNames) {
                try {
                    Path path = Paths.get(uploadDir, savedName);
                    Files.deleteIfExists(path);
                    dao.fileDeleteUrl(savedName); // file_url 기준으로 삭제
                } catch (Exception ex) {
                    ex.printStackTrace();
                    log.error("파일 시스템 삭제 실패: " + savedName, ex);
                }
            }
            return false;
        }
    
    }

	public boolean postUpdate(PostDTO dto) {
	    int row = dao.postUpdate(dto);
	    return row>0;
	}

	private void deleteFileIdx(String savedName) { // 시스템에서 파일 삭제하기
	    try {
	        Path path = Paths.get(uploadDir, savedName); // 파일 삭제하기
	        Files.deleteIfExists(path);
	    } catch (Exception e) {
	        log.error("파일 삭제 실패: " + savedName, e);
	    }
	}

    public boolean postDelete(PostDTO dto) {
        int row = dao.postDelete(dto);
        return row > 0;
    }

	public Map<String, Object> postDetail(int post_idx, boolean Hitup) {
		Map<String, Object> detailResult = new HashMap<>();
		
		if (Hitup) {
			dao.upHit(post_idx); // 상세보기 시 조회수 증가
		}
		
        PostDTO dto = dao.postDetail(post_idx);
        if (dto == null) {
            return detailResult;
        }
        
        detailResult.put("post", dto);
        detailResult.put("likes", dao.likeCnt(post_idx));
        detailResult.put("dislikes", dao.dislikeCnt(post_idx));
        List<Map<String, String>> photos = dao.fileList(post_idx);
        detailResult.put("photos", photos);

	    return detailResult;
	}
	
	public Map<String, Object> postList(int page, int board_idx, String sort) {
	    Map<String, Object> result = new HashMap<String, Object>();
	    result.put("page", page);
	    int offset = (page - 1) * post_count; // 페이지 시작 위치 계산

	    List<PostDTO> postList = dao.postList(offset, post_count, board_idx, sort); // 게시글 목록, 한 페이지 당 보여줄 게시글 수, 게시판 번호
	    int totalPosts = dao.postPages(board_idx);
	    int totalPages = (int) Math.ceil((double) totalPosts / post_count); // 한 페이지당 보여줄 게시글 개수로 나눈 후 올림 (double 값 정수 변환)
	    
	    List<Map<String, Object>> postMapList = new ArrayList<>();

	    for (PostDTO dto : postList) {
	        Map<String, Object> postMap = new HashMap<>();
	        postMap.put("post", dto);
	        postMap.put("likes", dao.likeCnt(dto.getPost_idx()));
	        postMap.put("dislikes", dao.dislikeCnt(dto.getPost_idx()));
            // 목록 보기 최적화를 위해 선택적으로 첫 번째 사진 또는 사진 수를 포함
	        List<Map<String, String>> photos = dao.fileList(dto.getPost_idx());
	        postMap.put("photos", photos);
	        postMapList.add(postMap);
	    }
	    
	    result.put("list", postMapList);
	    result.put("totalPages", totalPages);
	    result.put("totalPosts", totalPosts);

	    return result;
	}

	public boolean handleLike(LikeDislikeDTO dto) {
		String id = dto.getId(); // 같은 ID 인지 확인하려고 id 값 dto 에서 가져옴.
	    int post_idx = dto.getPost_idx();
	    int newType = dto.getLike_type(); // 추천 상태 확인

	    Integer currentType = dao.LikeType(id, post_idx); // Integer 가 아니라 int 면 null 값 지정 못함. (아무것도 안 눌렀을 때 상태)
	    
	    if (currentType == null) { // 처음에 null 이면 0 으로 변환시켜서 int 타입으로 설정
	        currentType = 0;
	    }
	    
	    if (currentType == newType) {
	        // 같은 상태 → 취소
	        return dao.likeDelete(id, post_idx) > 0;
	    } else if (currentType == 0) {
	        // 처음 → 삽입
	        return dao.likeInsert(dto) > 0;
	    } else {
	        // 다른 상태 → 업데이트
	        return dao.likeUpdate(dto) > 0;
	    } 
	}

	public List<Map<String, String>> fileList(int post_idx) {
		return dao.fileList(post_idx);
	}

	@Transactional
	public boolean fileDelete(String file_idx) {
	    // 1) 파일 정보 조회
	    Map<String, String> fileInfo = dao.fileInfo(file_idx);
	    if (fileInfo == null) {
	        return false; // 파일 존재하지 않음
	    }
	    
	    int row = dao.fileDelete(file_idx);
	    if (row>0) {
	    	deleteFileIdx(fileInfo.get("file_url"));
	    	return true;
	    }
	    return false;
	}

	public boolean updateFiles(int post_idx, MultipartFile[] files, List<String> keepFileIdx) {
		// 현재 게시글에 등록된 파일 리스트 조회
		List<Map<String, String>>currentFiles = dao.fileList(post_idx);
		
		// 삭제 대상 파일 결정 (keepFileIdx 에 없는 파일 삭제)
		for (Map<String, String> file : currentFiles) {
			String fileIdx = String.valueOf(file.get("file_idx")); // 비교를 위해 문자열인지 확인
			if (keepFileIdx == null || !keepFileIdx.contains(fileIdx)) {
				// DB 에서 삭제
				dao.fileDelete(fileIdx);
				// 실제 파일 삭제
				deleteFileIdx(file.get("file_url"));
			}
		}
		// 새 파일 저장
		if (files != null && files.length>0) {
			return saveFiles(post_idx,files); // savefiles 로직 재사용
		}
		return true; // 추가할 새 파일은 없지만 이전 파일이 성공적으로 삭제되었을 수 있음
	}

	public int userLevel(String loginId) {
		Integer level = dao.userLevel(loginId);
		return level != null ? level : -1; // 사용자를 찾을 수 없거나 레벨을 찾을 수 없는 경우 -1 또는 예외 발생
	}
	
	// 컨트롤러가 게시글 작성자 ID를 가져오기 위한 헬퍼 메소드
	public String postWriter(int post_idx) {
		return dao.postWriter(post_idx);
	}
}
