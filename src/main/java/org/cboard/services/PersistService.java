package org.cboard.services;

import java.io.File;
import java.net.URLDecoder;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.cboard.security.service.LocalSecurityFilter;
import org.cboard.services.persist.PersistContext;
import org.cboard.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by yfyuan on 2017/2/10.
 */
@Service
public class PersistService {

	private static final Logger LOG = LoggerFactory.getLogger(PersistService.class);

	// @Value("${phantomjs_path}")
	// private String phantomjsPath = new File(
	// this.getClass().getResource("/phantomjs/phantomjs-2.1.1-windows/bin/phantomjs.exe").getFile()).getPath();
	private String scriptPath = new File(this.getClass().getResource("/phantom.js").getFile()).getPath();

	@Value("${web_port}")
	private String webPort;
	@Value("${web_context}")
	private String webContext;

	@Autowired
	private HttpServletRequest request;

	private static final ConcurrentMap<String, PersistContext> TASK_MAP = new ConcurrentHashMap<>();

	public PersistContext persist(Long dashboardId, String userId) {
		String persistId = UUID.randomUUID().toString().replaceAll("-", "");
		Process process = null;
		try {
			String web = webPort + "/";
			if (StringUtils.isNotBlank(webContext)) {
				web += webContext + "/";
			}
			PersistContext context = new PersistContext(dashboardId);
			TASK_MAP.put(persistId, context);
			String uuid = UUID.randomUUID().toString().replaceAll("-", "");
			LocalSecurityFilter.put(uuid, userId);
			String phantomUrl = new StringBuffer(request.getScheme() + "://").append(request.getServerName() + ":")
					.append(web).append("render.html").append("?sid=").append(uuid).append("#?id=").append(dashboardId)
					.append("&pid=").append(persistId).toString();
			scriptPath = URLDecoder.decode(scriptPath, "UTF-8"); // decode
																	// whitespace

			String os = SystemUtil.getOsName();
			String phantomjsPath = null;
			if (os != null && os.toLowerCase().indexOf("linux") > -1) {
				phantomjsPath = new File(
						this.getClass().getResource("/phantomjs/phantomjs-2.1.1-linux-x86_64/bin/phantomjs").getFile())
								.getPath();
			} else if (os != null && os.toLowerCase().startsWith("win")) {
				phantomjsPath = new File(
						this.getClass().getResource("/phantomjs/phantomjs-2.1.1-windows/bin/phantomjs.exe").getFile())
								.getPath();
			}

			String cmd = String.format("%s %s %s", phantomjsPath, scriptPath, phantomUrl);
			LOG.info("Run phantomjs command: {}", cmd);
			process = Runtime.getRuntime().exec(cmd);
			synchronized (context) {
				context.wait(10 * 60 * 1000);
			}
			process.destroy();
			TASK_MAP.remove(persistId);
			return context;
		} catch (Exception e) {
			if (process != null) {
				process.destroy();
			}
			e.printStackTrace();
		}
		return null;
	}

	public String getPhantomUrl(Long dashboardId, String userId) {
		String persistId = UUID.randomUUID().toString().replaceAll("-", "");
		String web = webPort + "/";
		if (StringUtils.isNotBlank(webContext)) {
			web += webContext + "/";
		}
		PersistContext context = new PersistContext(dashboardId);
		TASK_MAP.put(persistId, context);
		String uuid = UUID.randomUUID().toString().replaceAll("-", "");
		LocalSecurityFilter.put(uuid, userId);
		String phantomUrl = new StringBuffer(request.getScheme() + "://").append(request.getServerName() + ":")
				.append(web).append("render.html").append("?sid=").append(uuid).append("#?id=").append(dashboardId)
				.append("&pid=").append(persistId).toString();
		return phantomUrl;
	}

	public String persistCallback(String persistId, JSONObject data) {
		PersistContext context = TASK_MAP.get(persistId);
		synchronized (context) {
			context.setData(data);
			context.notify();
		}
		return "1";
	}
}
