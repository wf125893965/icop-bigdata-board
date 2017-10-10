/**
 * 
 */
package org.cboard.security.service;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.cboard.util.CookiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.log.utils.ThreadCallerIdGenerator;

/**
 * @author wangFeng
 *
 */
public class SetTenantidFilter implements Filter {
	private static final Logger log = LoggerFactory.getLogger(SetTenantidFilter.class);

	protected void setTenantid(ServletRequest request, ServletResponse response) throws Exception {
		HttpServletRequest hReq = (HttpServletRequest) request;

		String tenantid = InvocationInfoProxy.getTenantid();
		Cookie cookie = CookiesUtil.getCookieByName(hReq, "tenantid");
		if (null != cookie) {
			tenantid = cookie.getValue();
		}

		if (StringUtils.isNotBlank(tenantid)) {
			try {
				InvocationInfoProxy.setTenantid(tenantid);
				this.initMDC();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else {
			// throw new Exception("tenantid为空！！！");
		}
	}

	private void initMDC() {
		String call_thread_id = InvocationInfoProxy.getCallid();
		if (StringUtils.isBlank(call_thread_id)) {
			call_thread_id = ThreadCallerIdGenerator.genCallerThreadId();
			InvocationInfoProxy.setCallid(call_thread_id);
		} else {
			MDC.put("call_thread_id", call_thread_id);
		}
		MDC.put("current_tenant_id", InvocationInfoProxy.getTenantid());
	}

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, javax.servlet.FilterChain chain)
			throws IOException, ServletException {
		try {
			this.setTenantid(request, response);
			chain.doFilter(request, response); // 执行目标资源，放行
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void destroy() {
	}

}
