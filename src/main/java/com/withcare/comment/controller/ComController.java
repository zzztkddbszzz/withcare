	package com.withcare.comment.controller;

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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import com.withcare.comment.dao.ComDAO;
import com.withcare.comment.dto.ComDTO;
import com.withcare.comment.service.ComService;
import com.withcare.util.JwtToken.JwtUtils;

@CrossOrigin
@RestController
public class ComController {
	
Logger log = LoggerFactory.getLogger(getClass());
	
	Map<String, Object> result = null;
	
	@Autowired ComService svc;
	@Autowired ComDAO dao;
	
	// WRITE COMMENT & MENTION
	@PostMapping("/post/detail/{post_idx}/write")
	public Map<String, Object> writeCom (@PathVariable int post_idx, @RequestBody ComDTO dto, @RequestHeader Map<String, String> header) {
		
		result = new HashMap<>();
		
		String id = (String) JwtUtils.readToken(header.get("authorization")).get("id"); // 작성자 정보 삽
		boolean login = false;
		
		if (id == null || id.isEmpty()) {
	        result.put("success", false);
	        result.put("message", "로그인이 필요합니다.");
	        result.put("loginYN", login);
	        return result;
	    }
	    
	    dto.setId(id);
	    dto.setPost_idx(post_idx);  // post_idx 설정 추가
	    
	    result = svc.writeCom(dto); // 서비스에서 댓글 허용 여부 체크 후 댓글 작성
	    login = true;
	    
		result.put("loginYN", login);
		
		return result;
	}
	
	
	// UPDATE COMMENT
	@PutMapping("/post/detail/{post_idx}/update")
	public Map<String, Object> editCom (@PathVariable int post_idx, @RequestBody ComDTO dto, @RequestHeader Map<String, String> header){
		
		result = new HashMap<>();
		
		String id = (String) JwtUtils.readToken(header.get("authorization")).get("id");
		boolean login = false;
		boolean success = false;
		
		dto.setId(id);
		
		if (id != null && !id.isEmpty()) { // 로그인 아이디랑 작성자 아이디는 svc에서 확인
			login = true;
			success = svc.updateCom(dto,id);
		}
		
		result.put("success", success);
		result.put("loginYN", login);
		
		return result;
	}
	
	// BLIND COMMENT
	@PutMapping("/post/detail/{post_idx}/delete")
	public Map<String, Object> delCom (@PathVariable int post_idx, @RequestBody ComDTO dto, @RequestHeader Map<String, String>header){

	    String id = (String) JwtUtils.readToken(header.get("authorization")).get("id");
	    boolean login = false;
	    
		result = new HashMap<String, Object>();
	    
	    boolean success = false;
	    if (id != null && !id.isEmpty()) { // 로그인 아이디랑 작성자 아이디는 svc에서 확인
	    	success = svc.delCom(dto, id);
	        login = true;
	    }

		result.put("idx", dto.getCom_idx());
		result.put("success", success);
	    result.put("loginYN", login);
	    
		return result;
	}
	
	// COMMENT LIST
	@GetMapping("/post/detail/{post_idx}/list")
	public Map<String, Object> comList(@PathVariable int post_idx) {

	    Map<String, Object> result = new HashMap<>();
	    result = svc.comList(post_idx);

	    return result;
	}
	
	// 멘션 기능 시 전체 유저 가져오는거에욤..
	@GetMapping("/post/detail/users")
	public Map<String, Object> getAllUsers() {
	    Map<String, Object> result = new HashMap<>();
	    List<String> userList = dao.selectId();  // 기존의 selectId 매퍼 사용
	    result.put("list", userList);
	    return result;
	}
	

	
	
	
	
	
	
	
	
	
	
	
}
