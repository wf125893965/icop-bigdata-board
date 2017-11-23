package org.cboard.security.service;

import java.util.ArrayList;

import org.cboard.dto.User;
import org.cboard.services.AuthenticationService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Created by yfyuan on 2016/12/14.
 */
public class DefaultAuthenticationService implements AuthenticationService {

    @Override
    public User getCurrentUser() {
        SecurityContext context = SecurityContextHolder.getContext();
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
