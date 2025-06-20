package com.withcare.member.dao;

import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LoginDAO {

	// int login(Map<String, String> params);

	Map<String, Object> login(Map<String, String> params);

	String findId(String name, String year, String email);

	String findPw(String id, String name, String year, String email);

	int updatePw(Map<String, String> params);

	int visitCnt(String id);

	int updateAccessDate(String id);

	int getLvIdx(String id);
	
	Map<String, Object> checkMemberStatus(Map<String, String> params);
}
