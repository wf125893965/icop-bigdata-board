package org.cboard.services;

import java.io.File;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URLDecoder;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.cboard.dao.BoardDao;
import org.cboard.dao.CategoryDao;
import org.cboard.exception.CBoardException;
import org.cboard.pojo.DashboardBoard;
import org.cboard.pojo.DashboardCategory;
import org.cboard.services.persist.PersistContext;
import org.cboard.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by yfyuan on 2017/2/10.
 */
@Service
public class PersistService {

	private static final Logger LOG = LoggerFactory.getLogger(PersistService.class);
	private String scriptPath = new File(this.getClass().getResource("/phantom.js").getFile()).getPath();
	private static final ConcurrentMap<String, PersistContext> TASK_MAP = new ConcurrentHashMap<>();

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private BoardDao boardDao;

	@Autowired
	private CategoryDao categoryDao;

	public PersistContext persist(Long dashboardId, String userId) {
		String persistId = UUID.randomUUID().toString().replaceAll("-", "");
		Process process = null;
		try {
			String web = request.getServerPort() + "";
			if (StringUtils.isNotBlank(request.getContextPath())) {
				web += request.getContextPath() + "/";
			}
			PersistContext context = new PersistContext(dashboardId);
			TASK_MAP.put(persistId, context);
			String uuid = UUID.randomUUID().toString().replaceAll("-", "");
			// LocalSecurityFilter.put(uuid, userId);
			String phantomUrl = new StringBuffer(request.getScheme() + "://").append(request.getServerName() + ":")
					.append(web).append("render.html").append("?sid=").append(uuid).append("#?id=").append(dashboardId)
					.append("&pid=").append(persistId).toString();
			scriptPath = URLDecoder.decode(scriptPath, "UTF-8"); // decode

			String os = SystemUtil.getOsName();
			String phantomjsPath = null;
			if (os != null && os.toLowerCase().indexOf("linux") > -1) {
				File f = new File(
						this.getClass().getResource("/phantomjs/phantomjs-2.1.1-linux-x86_64/bin/phantomjs").getFile());
				if (f.exists()) {
					LOG.info("Is Execute allow : {}", f.canExecute());
					phantomjsPath = f.getPath();
				}
				if (!f.canExecute()) {
					f.setExecutable(true);
				}
			} else if (os != null && os.toLowerCase().startsWith("win")) {
				phantomjsPath = new File(
						this.getClass().getResource("/phantomjs/phantomjs-2.1.1-windows/bin/phantomjs.exe").getFile())
								.getPath();
			}

			String cmd = String.format("%s %s %s", phantomjsPath, scriptPath, phantomUrl);
			LOG.info("Run phantomjs command: {}", cmd);
			process = Runtime.getRuntime().exec(cmd);
			final Process p = process;
			new Thread(() -> {
				InputStreamReader ir = new InputStreamReader(p.getInputStream());
				LineNumberReader input = new LineNumberReader(ir);
				String line;
				try {
					while ((line = input.readLine()) != null) {
						LOG.info(line);
					}
					LOG.info("Finished command " + cmd);
				} catch (Exception e) {
					LOG.error("Error", e);
					p.destroy();
				}
			}).start();
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
			LOG.error(getClass().getName(), e);
			throw new CBoardException(e.getMessage());
		}
	}

	public String getPhantomUrl(Long dashboardId, String userId) {
		String web = request.getServerPort() + "";
		if (StringUtils.isNotBlank(request.getContextPath())) {
			web += request.getContextPath() + "/";
		}
		DashboardBoard board = boardDao.getBoard(dashboardId);
		List<DashboardCategory> categoryList = categoryDao.getCategoryList();
		String categoryName = null;
		if (!categoryList.isEmpty()) {
			for (DashboardCategory dc : categoryList) {
				if (dc.getId() == board.getCategoryId()) {
					categoryName = dc.getName();
				}
			}
		}
		String basePath = request.getScheme() + "://" + request.getServerName() + ":" + web;
		String phantomUrl = basePath + "board.html#/dashboard/" + categoryName + "/" + dashboardId;
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
