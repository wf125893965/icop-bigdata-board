package org.cboard.controller;

import java.util.ArrayList;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.StringUtils;
import org.cboard.dao.UserDao;
import org.cboard.dto.User;
import org.cboard.pojo.DashboardUser;
import org.cboard.security.ShareAuthenticationToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.encoding.Md5PasswordEncoder;
import org.springframework.security.authentication.encoding.PasswordEncoder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {
	
	public static final PasswordEncoder encoder = new Md5PasswordEncoder(); 
	
	@Autowired
	private UserDao userDao;

    @RequestMapping(value = "/login", method = RequestMethod.GET)
    public String loginPage() {
        return "login";
    }
    
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String doLogin(HttpServletRequest request,@RequestParam(name = "username") String username, @RequestParam(name = "password") String password) {
    	String encodePassword = encoder.encodePassword(password, null);
    	DashboardUser dashboardUser = userDao.getUserByLoginName(username);
    	if(dashboardUser!=null&&dashboardUser.getUserPassword().equals(encodePassword)){
    		SecurityContext context = SecurityContextHolder.getContext();
			User user = new User(username, encodePassword, new ArrayList<>());
			user.setUserId("1");
			context.setAuthentication(new ShareAuthenticationToken(user));
			request.getSession().setAttribute("SPRING_SECURITY_CONTEXT", context);
    		return "redirect:/starter.html";
    	}
        return "login";
    }

    private String getPrincipal(){
        String userName = null;
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof UserDetails) {
            userName = ((UserDetails)principal).getUsername();
        } else {
            userName = principal.toString();
        }
        return userName;
    }

}