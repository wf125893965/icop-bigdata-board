/**
 * 
 */
package org.cboard.security.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;

import com.yonyou.iuap.auth.token.ITokenProcessor;
import com.yonyou.iuap.auth.token.TokenFactory;
import com.yonyou.iuap.auth.token.TokenParameter;
import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.log.utils.ThreadCallerIdGenerator;
import com.yonyou.iuap.utils.CookieUtil;
import com.yonyou.iuap.utils.PropertyUtil;

import net.sf.json.JSONObject;

/**
 * @author wangFeng
 *
 */
public class StatelessAuthcFilter implements Filter {
	private static final Logger log = LoggerFactory.getLogger(StatelessAuthcFilter.class);
	public static final int HTTP_STATUS_AUTH = 306;
	private String sysid;
	@Autowired
	private TokenFactory tokenFactory;
	private String[] esc = new String[] { "/logout", "/login", "/formLogin", ".jpg", ".png", ".gif", ".css", ".js",
			".jpeg", "/oauth_login", "/oauth_approval" };
	private List<String> excludCongtextKeys = Arrays
			.asList(new String[] { "u_sysid", "tenantid", "u_callid", "u_usercode", "token", "u_logints", "u_locale",
					"u_theme", "u_timezone", "current_user_name", "call_thread_id", "current_tenant_id" });

	public void setSysid(String sysid) {
		this.sysid = sysid;
	}

	public void setTokenFactory(TokenFactory tokenFactory) {
		this.tokenFactory = tokenFactory;
	}

	public void setEsc(String[] esc) {
		this.esc = esc;
	}

	public void setExcludCongtextKeys(List<String> excludCongtextKeys) {
		this.excludCongtextKeys = excludCongtextKeys;
	}

	protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue)
			throws Exception {
		return false;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
		boolean isAjax = this.isAjax(request);
		HttpServletRequest hReq = (HttpServletRequest) request;
		Cookie[] cookies = hReq.getCookies();
		String authority = hReq.getHeader("Authority");
		String logints;
		String username;
		if (StringUtils.isNotBlank(authority)) {
			HashSet tokenStr = new HashSet();
			String[] cookieUserName = authority.split(";");
			String[] theme = cookieUserName;
			int locale = cookieUserName.length;

			for (int timeZone = 0; timeZone < locale; ++timeZone) {
				logints = theme[timeZone];
				String[] callerThreadId = logints.split("=");
				username = StringUtils.trim(callerThreadId[0]);
				String needCheck = StringUtils.trim(callerThreadId[1]);
				Cookie params = new Cookie(username, needCheck);
				tokenStr.add(params);
			}

			cookies = (Cookie[]) tokenStr.toArray(new Cookie[0]);
		}

		String token = CookieUtil.findCookieValue(cookies, "token");
		String usercode = CookieUtil.findCookieValue(cookies, "u_usercode");
		username = request.getParameter("u_usercode");
		if (username == null && StringUtils.isNotBlank(usercode)) {
			username = usercode;
		}

		boolean arg28 = !this.include(hReq);
		if (!arg28) {
			return true;
		} else if (token != null && username != null) {
			ITokenProcessor tokenProcessor = this.tokenFactory.getTokenProcessor(token);
			TokenParameter tp = tokenProcessor.getTokenParameterFromCookie(cookies);
			try {
				InvocationInfoProxy.setSysid(this.sysid);

				InvocationInfoProxy.setUserid(username);
				InvocationInfoProxy.setTenantid((String) tp.getExt().get("tenantid"));
				InvocationInfoProxy.setToken(token);
				this.initExtendParams(cookies);
				this.initMDC();
				this.afterValidate(hReq);
				return true;
			} catch (Exception e) {
				log.error(e.getMessage(), e);
				if (isAjax && e instanceof AuthenticationException) {
					this.onAjaxAuthFail(request, response);
					return false;
				} else {
					this.onLoginFail(request, response);
					return false;
				}
			}
		} else {
			if (isAjax) {
				this.onAjaxAuthFail(request, response);
			} else {
				this.onLoginFail(request, response);
			}

			return false;
		}
	}

	private boolean isAjax(ServletRequest request) {
		boolean isAjax = false;
		if (request instanceof HttpServletRequest) {
			HttpServletRequest rq = (HttpServletRequest) request;
			String requestType = rq.getHeader("X-Requested-With");
			if (requestType != null && "XMLHttpRequest".equals(requestType)) {
				isAjax = true;
			}
		}

		return isAjax;
	}

	protected void onAjaxAuthFail(ServletRequest request, ServletResponse resp) throws IOException {
		HttpServletResponse response = (HttpServletResponse) resp;
		JSONObject json = new JSONObject();
		json.put("msg", "auth check error!");
		response.setStatus(306);
		response.getWriter().write(json.toString());
	}

	protected void onLoginFail(ServletRequest request, ServletResponse response) throws IOException {
		HttpServletResponse httpResponse = (HttpServletResponse) response;
		httpResponse.setStatus(306);
		this.redirectToLogin(request, httpResponse);
	}

	protected void redirectToLogin(ServletRequest request, ServletResponse response) throws IOException {
		HttpServletRequest hReq = (HttpServletRequest) request;
		String rURL = hReq.getRequestURI();
		String qryString = hReq.getQueryString();
		if (qryString != null && !qryString.isEmpty()) {
			(new StringBuilder()).append(qryString).append("?").append(hReq.getQueryString()).toString();
		}

		rURL = Base64.encodeBase64URLSafeString(rURL.getBytes());
	}

	public boolean include(HttpServletRequest request) {
		String u = request.getRequestURI();
		String[] exeludeStr = this.esc;
		int customExcludes = exeludeStr.length;

		for (int arr$ = 0; arr$ < customExcludes; ++arr$) {
			String len$ = exeludeStr[arr$];
			if (u.endsWith(len$)) {
				return true;
			}
		}

		String arg8 = PropertyUtil.getPropertyByKey("filter_excludes");
		if (StringUtils.isNotBlank(arg8)) {
			String[] arg9 = arg8.split(",");
			String[] arg10 = arg9;
			int arg11 = arg9.length;

			for (int i$ = 0; i$ < arg11; ++i$) {
				String e = arg10[i$];
				if (u.endsWith(e)) {
					return true;
				}
			}
		}

		return false;
	}

	public void afterCompletion(ServletRequest request, ServletResponse response, Exception exception)
			throws Exception {
		InvocationInfoProxy.reset();
		this.clearMDC();
	}

	private void initExtendParams(Cookie[] cookies) {
		Cookie[] arr$ = cookies;
		int len$ = cookies.length;

		for (int i$ = 0; i$ < len$; ++i$) {
			Cookie cookie = arr$[i$];
			String cname = cookie.getName();
			String cvalue = cookie.getValue();
			if (!this.excludCongtextKeys.contains(cname)) {
				InvocationInfoProxy.setParameter(cname, cvalue);
			}
		}

	}

	private void initMDC() {
		String username = "";
		Subject subject = SecurityUtils.getSubject();
		if (subject != null && subject.getPrincipal() != null) {
			username = (String) SecurityUtils.getSubject().getPrincipal();
		}

		MDC.put("current_user_name", username);
		String call_thread_id = InvocationInfoProxy.getCallid();
		if (StringUtils.isBlank(call_thread_id)) {
			call_thread_id = ThreadCallerIdGenerator.genCallerThreadId();
			InvocationInfoProxy.setCallid(call_thread_id);
		} else {
			MDC.put("call_thread_id", call_thread_id);
		}

		MDC.put("current_tenant_id", InvocationInfoProxy.getTenantid());
		this.initCustomMDC();
	}

	protected void initCustomMDC() {
	}

	protected void afterValidate(HttpServletRequest hReq) {
	}

	protected void clearMDC() {
		MDC.remove("current_user_name");
		MDC.remove("call_thread_id");
		MDC.remove("current_tenant_id");
		this.clearCustomMDC();
	}

	protected void clearCustomMDC() {
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, javax.servlet.FilterChain chain)
			throws IOException, ServletException {
		try {
			this.onAccessDenied(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {
	}

}
