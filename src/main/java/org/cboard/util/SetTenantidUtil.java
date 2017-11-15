/**
 * 
 */
package org.cboard.util;

import javax.servlet.ServletRequest;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import com.yonyou.iuap.context.InvocationInfoProxy;
import com.yonyou.iuap.log.utils.ThreadCallerIdGenerator;

/**
 * @author wangFeng
 *
 */
public class SetTenantidUtil {
	private static final Logger log = LoggerFactory.getLogger(SetTenantidUtil.class);

	public static void setTenantid(ServletRequest request) throws Exception {
		HttpServletRequest hReq = (HttpServletRequest) request;

		String tenantid = InvocationInfoProxy.getTenantid();
		Cookie cookie = CookiesUtil.getCookieByName(hReq, "tenantid");
		if (null != cookie) {
			tenantid = cookie.getValue();
		}

		if (StringUtils.isNotBlank(tenantid)) {
			try {
				InvocationInfoProxy.setTenantid(tenantid);
				initMDC();
			} catch (Exception e) {
				log.error(e.getMessage(), e);
			}
		} else {
			// throw new Exception("tenantid为空！！！");
		}
	}

	public static void initMDC() {
		String call_thread_id = InvocationInfoProxy.getCallid();
		if (StringUtils.isBlank(call_thread_id)) {
			call_thread_id = ThreadCallerIdGenerator.genCallerThreadId();
			InvocationInfoProxy.setCallid(call_thread_id);
		} else {
			MDC.put("call_thread_id", call_thread_id);
		}
		MDC.put("current_tenant_id", InvocationInfoProxy.getTenantid());
	}
}
