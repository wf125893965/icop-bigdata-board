package org.cboard.security.service;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.cboard.dto.User;
import org.cboard.security.ShareAuthenticationToken;
import org.cboard.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Created by yfyuan on 2016/12/14.
 */
public class DefaultAuthenticationService implements AuthenticationService {

	@Autowired
	private HttpServletRequest request;

	@Override
	public User getCurrentUser() {

		User user = null;
		SecurityContext context = (SecurityContext) request.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
		if (context == null) {
			context = SecurityContextHolder.getContext();
			user = new User("shareUser", "", new ArrayList<>());
			user.setUserId("1");
			context.setAuthentication(new ShareAuthenticationToken(user));
			request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", context);
			return user;
		}else {
			Authentication authentication = context.getAuthentication();
			if(CasAuthenticationToken.class.isInstance(authentication)){
				authentication = (CasAuthenticationToken)authentication;
				
			}else if(ShareAuthenticationToken.class.isInstance(authentication)){
				authentication = (ShareAuthenticationToken)authentication;
			}
			user = (User) authentication.getPrincipal();
		}
		return user;
		/*
		 * Authentication authentication = context.getAuthentication(); if
		 * (authentication == null) { User user = new User("shareUser", "", new
		 * ArrayList<>()); user.setUserId("1"); return user; } User user =
		 * (User) authentication.getPrincipal(); if (user == null) { user = new
		 * User("shareUser", "", new ArrayList<>()); user.setUserId("1"); return
		 * user; }
		 */
	}

}
