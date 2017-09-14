package org.cboard.security.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.cboard.dto.User;
import org.cboard.security.ShareAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * Created by yfyuan on 2017/2/22.
 */
public class LocalSecurityFilter implements Filter {

	private static LoadingCache<String, String> sidCache = CacheBuilder.newBuilder()
			.expireAfterAccess(1, TimeUnit.MINUTES).build(new CacheLoader<String, String>() {
				@Override
				public String load(String key) throws Exception {
					return null;
				}
			});

	public static void put(String sid, String uid) {
		sidCache.put(sid, uid);
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {

	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
			throws IOException, ServletException {
		SecurityContext context = (SecurityContext) ((HttpServletRequest) servletRequest).getSession()
				.getAttribute("SPRING_SECURITY_CONTEXT");
		Authentication authentication = null;
		if(context!=null){
			authentication = context.getAuthentication();
		}
		if ("/render.html".equals(((HttpServletRequest) servletRequest).getServletPath())) {
			if (authentication == null || ("shareUser").equals(authentication.getName())) {
				User user = new User("shareUser", "", new ArrayList<>());
				user.setUserId("1");
				context.setAuthentication(new ShareAuthenticationToken(user));
				((HttpServletRequest) servletRequest).getSession().setAttribute("SPRING_SECURITY_CONTEXT", context);
			}
		} else {
			if (authentication != null && ("shareUser").equals(authentication.getName())) {
				((HttpServletRequest) servletRequest).getSession().setAttribute("SPRING_SECURITY_CONTEXT", null);
			}
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	@Override
	public void destroy() {

	}
}
