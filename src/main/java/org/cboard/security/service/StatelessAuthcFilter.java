/**
 * 
 */
package org.cboard.security.service;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.cboard.util.CookiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.log.utils.ThreadCallerIdGenerator;

import net.sf.json.JSONObject;

/**
 * @author wangFeng
 *
 */
public class StatelessAuthcFilter implements Filter {
	private static final Logger log = LoggerFactory.getLogger(StatelessAuthcFilter.class);
	public static final int HTTP_STATUS_AUTH = 306;
	private List<String> excludCongtextKeys = Arrays
			.asList(new String[] { "u_sysid", "tenantid", "u_callid", "u_usercode", "token", "u_logints", "u_locale",
					"u_theme", "u_timezone", "current_user_name", "call_thread_id", "current_tenant_id" });

	public void setExcludCongtextKeys(List<String> excludCongtextKeys) {
		this.excludCongtextKeys = excludCongtextKeys;
	}

	protected void setTenantid(ServletRequest request, ServletResponse response) throws Exception {
		HttpServletRequest hReq = (HttpServletRequest) request;
		Cookie[] cookies = hReq.getCookies();

		String tenantid = "";
		Cookie cookie = CookiesUtil.getCookieByName(hReq, "tenantid");
		if (null != cookie) {
			tenantid = cookie.getValue();
		}

		if (StringUtils.isNotBlank(tenantid)) {
			try {
				InvocationInfoProxy.setTenantid(tenantid);
				this.initExtendParams(cookies);
				this.initMDC();
				this.afterValidate(hReq);
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else {
			throw new Exception("tenantid为空！！！");
		}
	}

	protected void onAjaxAuthFail(ServletRequest request, ServletResponse resp) throws IOException {
		HttpServletResponse response = (HttpServletResponse) resp;
		JSONObject json = new JSONObject();
		json.put("msg", "auth check error!");
		response.setStatus(306);
		response.getWriter().write(json.toString());
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
			this.setTenantid(request, response);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {
	}

}
