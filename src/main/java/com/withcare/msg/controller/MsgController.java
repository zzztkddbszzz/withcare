package com.withcare.msg.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.withcare.msg.dto.MsgDTO;
import com.withcare.msg.service.MsgService;
import com.withcare.util.JwtToken;

@CrossOrigin
@RestController
public class MsgController {

	Logger log = LoggerFactory.getLogger(getClass());

	@Autowired MsgService svc;

	// SEND MSG
	@PostMapping("/msg/send")
	public Map<String, Object> sendMsg(@RequestBody MsgDTO dto, @RequestHeader Map<String, String> header) {

		Map<String, Object> resp = new HashMap<>();

		String loginId = (String) JwtToken.JwtUtils.readToken(header.get("authorization")).get("id");
		boolean login = false;

		if (!loginId.equals("") && loginId.equals(dto.getSender_id())) {
			dto.setSender_id(loginId);
			svc.sendMsg(dto);
			login = true;
		}

		resp.put("loginYN", login);
		resp.put("message", login ? "📫 쪽지가 성공적으로 전송되었습니다." : "❌ 로그인 정보가 일치하지 않습니다.");

		return resp;
	}

	// OUTBOX
	@GetMapping("/msg/outbox/{id}/{page}")
	public Map<String, Object> outbox(@PathVariable String id, @PathVariable int page, @RequestHeader Map<String, String> header) {
		
		Map<String, Object> resp = new HashMap<>();
		String loginId = (String) JwtToken.JwtUtils.readToken(header.get("authorization")).get("id");
		boolean login = false;

		if (!loginId.equals("") && loginId.equals(id)) {
			Map<String, Object> list = svc.outbox(id, page);
			resp.put("outbox", list);
			login = true;
		}

		resp.put("loginYN", login);
		return resp;
	}

	// INBOX
	@GetMapping("/msg/inbox/{id}")
	public Map<String, Object> inbox(@PathVariable String id, @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size, @RequestHeader Map<String, String> header) {
	    
	    log.info("받은 쪽지 요청 - id: {}, page: {}, size: {}", id, page, size);
	    
	    Map<String, Object> resp = new HashMap<>();
	    String loginId = (String) JwtToken.JwtUtils.readToken(header.get("authorization")).get("id");
	    boolean login = false;

	    if (!loginId.equals("") && loginId.equals(id)) {
	        // 전체 메시지 수 조회
	        int totalCount = svc.getInboxCnt(id);
	        // 전체 페이지 수 계산
	        int totalPages = (int) Math.ceil((double) totalCount / size);
	        
	        List<MsgDTO> list = svc.inbox(id, page, size);
	        resp.put("inbox", list);
	        resp.put("pages", totalPages);  // 전체 페이지 수
	        resp.put("currentPage", page + 1);  // 프론트엔드는 1부터 시작하므로 +1
	        login = true;
	    }

	    resp.put("loginYN", login);
	    return resp;
	}

    // SAVED INBOX (보관된 쪽지함)
    @GetMapping("/msg/inbox/saved/{id}")
    public Map<String, Object> savedInbox(@PathVariable String id, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size, @RequestHeader Map<String, String> header) {
        
        log.info("보관된 쪽지 요청 - id: {}, page: {}, size: {}", id, page, size);
        
        Map<String, Object> resp = new HashMap<>();
        String loginId = (String) JwtToken.JwtUtils.readToken(header.get("authorization")).get("id");
        boolean login = false;

        if (!loginId.equals("") && loginId.equals(id)) {
            // 전체 보관된 메시지 수 조회
            int totalCount = svc.getSavedInboxCnt(id);
            // 전체 페이지 수 계산
            int totalPages = (int) Math.ceil((double) totalCount / size);
            
            List<MsgDTO> list = svc.savedInbox(id, page, size);
            resp.put("inbox", list);
            resp.put("pages", totalPages);  // 전체 페이지 수
            resp.put("currentPage", page + 1);  // 프론트엔드는 1부터 시작하므로 +1
            login = true;
        }

        resp.put("loginYN", login);
        return resp;
    }

	// MSG DETAIL
	@GetMapping("/msg/detail/{id}/{msg_idx}")
	public Map<String, Object> msgDetail(@PathVariable String id, @PathVariable int msg_idx,
			@RequestHeader Map<String, String> header) {
		
		Map<String, Object> resp = new HashMap<>();
		String loginId = (String) JwtToken.JwtUtils.readToken(header.get("authorization")).get("id");
		boolean login = false;

		if (!loginId.equals("") && loginId.equals(id)) {
			MsgDTO dto = svc.msgDetail(msg_idx);
			
		// 읽음 처리 (수신자가 읽었을 때만 처리)
		if(dto.getReceiver_id().equals(loginId) && !dto.isMsg_read()) {
			svc.readYN(msg_idx);
			dto.setMsg_read(true);
		}
			
			resp.put("msg", dto);
			login = true;
		}

		resp.put("loginYN", login);
		return resp;
	}

	// DEL MSG (INBOX 기준)
	@PutMapping("/msg/delete/inbox/{id}/{msg_idx}")
	public Map<String, Object> msgDel(@PathVariable String id, @PathVariable int msg_idx,
			@RequestHeader Map<String, String> header) {
		Map<String, Object> resp = new HashMap<>();
		String loginId = (String) JwtToken.JwtUtils.readToken(header.get("authorization")).get("id");
		boolean login = false;

		if (!loginId.equals("") && loginId.equals(id)) {
			svc.msgDel(msg_idx);
			resp.put("message", "✉️ 받은 쪽지가 삭제 처리 되었습니다.");
			login = true;
		}

		resp.put("loginYN", login);
		return resp;
	}

	// DEL MSG (OUTBOX 기준)
	@PutMapping("/msg/delete/outbox/{id}/{msg_idx}")
	public Map<String, Object> msgDelOut(@PathVariable String id, @PathVariable int msg_idx,
			@RequestHeader Map<String, String> header) {
		Map<String, Object> resp = new HashMap<>();
		String loginId = (String) JwtToken.JwtUtils.readToken(header.get("authorization")).get("id");
		boolean login = false;

		if (!loginId.equals("") && loginId.equals(id)) {
			svc.msgDelOut(msg_idx);
			resp.put("message", "✉️ 보낸 쪽지가 삭제 처리 되었습니다.");
			login = true;
		}

		resp.put("loginYN", login);
		return resp;
	}

	// SAVE MSG (INBOX 기준)
	@PutMapping("/msg/save/inbox/{id}/{msg_idx}")
	public Map<String, Object> msgSave(@PathVariable String id, @PathVariable int msg_idx,
			@RequestHeader Map<String, String> header) {
		Map<String, Object> resp = new HashMap<>();
		String loginId = (String) JwtToken.JwtUtils.readToken(header.get("authorization")).get("id");
		boolean login = false;

		if (!loginId.equals("") && loginId.equals(id)) {
			svc.msgSave(msg_idx);
			resp.put("message", "📬 받은 쪽지가 보관 처리 되었습니다.");
			login = true;
		}

		resp.put("loginYN", login);
		return resp;
	}

	// SAVE MSG (OUTBOX 기준)
	@PutMapping("/msg/save/outbox/{id}/{msg_idx}")
	public Map<String, Object> msgSaveOut(@PathVariable String id, @PathVariable int msg_idx,
			@RequestHeader Map<String, String> header) {
		Map<String, Object> resp = new HashMap<>();
		String loginId = (String) JwtToken.JwtUtils.readToken(header.get("authorization")).get("id");
		boolean login = false;

		if (!loginId.equals("") && loginId.equals(id)) {
			svc.msgSaveOut(msg_idx);
			resp.put("message", "📬️ 보낸 쪽지가 보관 처리 되었습니다.");
			login = true;
		}

		resp.put("loginYN", login);
		return resp;
	}

}