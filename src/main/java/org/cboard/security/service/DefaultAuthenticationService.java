package org.cboard.security.service;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.cboard.dto.User;
import org.cboard.services.AuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
/**
 * Created by yfyuan on 2016/12/14.
 */
public class DefaultAuthenticationService implements AuthenticationService {

	@Autowired
    private HttpServletRequest request;
	
	@Override
    public User getCurrentUser() {
    	
    	SecurityContext context = (SecurityContext) request.getSession().getAttribute("SPRING_SECURITY_CONTEXT");
        if (context == null) {
            return null;
        }
        Authentication authentication = context.getAuthentication();
        if (authentication == null) {
			User user = new User("shareUser", "", new ArrayList<>());
			user.setUserId("1");
			return user;
        }
        User user = (User) authentication.getPrincipal();
        if (user == null) {
        	user = new User("shareUser", "", new ArrayList<>());
			user.setUserId("1");
			return user;
        }
        return user;
    }

}
