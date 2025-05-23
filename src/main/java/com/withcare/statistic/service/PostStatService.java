package com.withcare.statistic.service;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.withcare.statistic.dao.PostStatDAO;
import com.withcare.statistic.dto.PostBestStatDTO;
import com.withcare.statistic.dto.PostStatDTO;

@Service
public class PostStatService {

	Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	PostStatDAO dao;

	public int getWeeklyPostCount() {
		return dao.getWeeklyPostCount();
	}

	public int getWeeklyCommentCount() {
		return dao.getWeeklyCommentCount();
	}

	public List<PostStatDTO> getPostAndCom() {
		return dao.getPostAndCom();
	}

	public List<PostBestStatDTO> getBestPost() {
		return dao.getBestPost();
	}
}
