package com.withcare.comment.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.withcare.comment.dao.ComDAO;
import com.withcare.comment.dto.ComDTO;
import com.withcare.comment.dto.MenDTO;
import com.withcare.noti.service.NotiService;

@Service
public class ComService {

    Logger log = LoggerFactory.getLogger(getClass());

    Map<String, Object> result = null;

    @Autowired
    ComDAO dao;

    @Autowired
    NotiService notiService;

    // WRITE COMMENT

    public Map<String, Object> writeCom(ComDTO dto) {
        result = new HashMap<>();
        
        // 게시글의 댓글 허용 여부 체크
        Boolean isCommentAllowed = dao.getPostCommentPermission(dto.getPost_idx());
        if (isCommentAllowed == null || !isCommentAllowed) {
            result.put("success", false);
            result.put("message", "댓글이 허용되지 않은 게시글입니다.");
            return result;
        }

        // 댓글 작성
        int row = dao.writeCom(dto);
        if (row > 0) {
            // 댓글 작성 성공 시 필요한 정보 수집
            int com_idx = dto.getCom_idx();
            int post_idx = dto.getPost_idx();
            String writer_id = dto.getId();
            String post_writer_id = dao.getPostWriterId(dto.getPost_idx());

            // 게시글의 익명 여부 확인
            Boolean isAnonymous = dao.getPostAnonymousYn(post_idx);

            // 댓글 내용에서 멘션된 아이디 조회
            String content = dto.getCom_content();
            Set<String> mentionId = getId(content);

            // 멘션 정보 저장
            for (String menId : mentionId) {
                MenDTO menDto = new MenDTO();
                menDto.setCom_idx(dto.getCom_idx());
                menDto.setMen_id(menId);
                menDto.setMen_writer_id(dto.getId());
                menDto.setMen_content(content);
                menDto.setMen_blind_yn(false);

                dao.writeMention(menDto);
            }

            // 알림 전송 로직 개선
            sendNotifications(com_idx, post_idx, writer_id, post_writer_id, mentionId, isAnonymous);

            result.put("success", true);
            result.put("idx", dto.getCom_idx());
            result.put("mentioned", mentionId);
        } else {
            result.put("success", false);
            result.put("message", "댓글 등록에 실패했습니다.");
            result.put("mentioned", new HashSet<String>());
        }

        return result;
    }

    /**
     * 중복 알림을 방지하면서 적절한 알림을 전송하는 메서드
     */
    private void sendNotifications(int com_idx, int post_idx, String writer_id, 
                                  String post_writer_id, Set<String> mentionIds, Boolean isAnonymous) {
        
        // 본인이 작성한 댓글은 알림 제외
        if (writer_id.equals(post_writer_id)) {
            // 게시글 작성자가 본인 게시글에 댓글을 단 경우
            // 멘션된 사용자들에게만 멘션 알림 전송
            for (String menId : mentionIds) {
                if (!menId.equals(writer_id)) { // 본인 멘션 제외
                    notiService.sendMentionNoti(com_idx, post_idx, writer_id, menId, isAnonymous);
                }
            }
        } else {
            // 다른 사람의 게시글에 댓글을 단 경우
            boolean postWriterMentioned = mentionIds.contains(post_writer_id);
            
            if (postWriterMentioned) {
                // 게시글 작성자가 멘션된 경우: 멘션 알림만 전송 (중복 방지)
                notiService.sendMentionNoti(com_idx, post_idx, writer_id, post_writer_id, isAnonymous);
                
                // 게시글 작성자를 제외한 다른 멘션된 사용자들에게 멘션 알림 전송
                for (String menId : mentionIds) {
                    if (!menId.equals(writer_id) && !menId.equals(post_writer_id)) {
                        notiService.sendMentionNoti(com_idx, post_idx, writer_id, menId, isAnonymous);
                    }
                }
            } else {
                // 게시글 작성자가 멘션되지 않은 경우: 댓글 알림 전송
                notiService.sendCommentNoti(com_idx, post_idx, writer_id, post_writer_id, isAnonymous);
                
                // 멘션된 사용자들에게 멘션 알림 전송
                for (String menId : mentionIds) {
                    if (!menId.equals(writer_id)) { // 본인 멘션 제외
                        notiService.sendMentionNoti(com_idx, post_idx, writer_id, menId, isAnonymous);
                    }
                }
            }
        }
    }

    // GET MENTIONED ID
    private Set<String> getId(String content) {
        List<String> memberIdList = dao.selectId(); // DB에서 멘션된 아이디 가져오기
        Set<String> mentioned = new HashSet<>(); // 멘션된 아이디 저장할 set

        for (String memberId : memberIdList) {
            if (content.contains("@" + memberId)) { // @ 뒤에 아이디 오는지 확인
                mentioned.add(memberId); // 존재하는 아이디면 set에 추가
            }
        }

        return mentioned; // 멘션된 아이디 반환
    }

    // UPDATE COMMENT
    public boolean updateCom(ComDTO dto, String id) {

        String writerId = dao.writerId(dto.getCom_idx());

        if (writerId == null || (!writerId.equals(id) && dao.userLevel(id) != 7)) { // null 값 들어오거나 토큰 아이디 = 작성자 아이디 같지 않거나 관리자 레벨 아니면 false
            return false;
        }

        int row = dao.updateCom(dto);
        return row > 0;
    }

    // DELETE COMMENT
    public boolean delCom(ComDTO dto, String id) {
        String writerId = dao.writerId(dto.getCom_idx());
        if (id == null) {
            return false;
        }

        if (writerId == null || (!writerId.equals(id) && dao.userLevel(id) != 7)) {
            return false;
        }

        dto.setCom_blind_yn(true);  // 명시적으로 blind 처리
        int row = dao.delCom(dto);
        
        // 댓글에 연결된 멘션이 있다면 함께 블라인드 처리
        if (row > 0) {
            dao.blindMenIfExist(dto.getCom_idx());
        }
        
        return row > 0;
    }

    // COMMENT LIST
   public Map<String, Object> comList(int post_idx) {

        result = new HashMap<>();

        List<ComDTO> comList = dao.comList(post_idx);
        int comments = dao.comCnt(post_idx);

        result.put("list", comList);
        result.put("comments", comments);

        return result;
    }


}