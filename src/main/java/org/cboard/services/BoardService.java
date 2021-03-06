package org.cboard.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.cboard.dao.BoardDao;
import org.cboard.dao.CategoryDao;
import org.cboard.dao.WidgetDao;
import org.cboard.dto.ViewDashboardBoard;
import org.cboard.dto.ViewDashboardWidget;
import org.cboard.pojo.DashboardBoard;
import org.cboard.pojo.DashboardCategory;
import org.cboard.pojo.DashboardWidget;
import org.cboard.services.persist.PersistContext;
import org.cboard.services.persist.excel.XlsProcessService;
import org.cboard.util.SystemUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.yyjz.icop.support.api.service.FunctionDubboService;
import com.yyjz.icop.support.vo.FunctionVO;

/**
 * Created by yfyuan on 2016/8/23.
 */
@Repository
public class BoardService {

	private Logger LOG = LoggerFactory.getLogger(this.getClass());
	@Autowired
	private FunctionDubboService functionDubboService;

	@Autowired
	private HttpServletRequest request;

	@Value("${module.id.report}")
	private String publishModuleId;

	private String divClass = "patch-material";

	private String xmlTemplate = "base";

	private String publishType = "type-report";

	@Autowired
	private BoardDao boardDao;

	@Autowired
	private CategoryDao categoryDao;

	@Autowired
	private WidgetDao widgetDao;

	@Autowired
	private PersistService persistService;

	@Autowired
	private XlsProcessService xlsProcessService;

	public List<DashboardBoard> getBoardList(String userId) {
		return boardDao.getBoardList(userId);
	}

	public ServiceStatus publishBoard(String userId, Long id) {
		try {
			DashboardBoard board = boardDao.getBoard(id);
			List<DashboardCategory> categoryList = categoryDao.getCategoryList();
			String categoryName = null;
			if (!categoryList.isEmpty()) {
				for (DashboardCategory dc : categoryList) {
					if (dc.getId() == board.getCategoryId()) {
						categoryName = dc.getName();
					}
				}
			}

			List<FunctionVO> functions = new ArrayList<FunctionVO>();
			FunctionVO function = new FunctionVO();
			String randomNumber = SystemUtil.getRandomNumber();
			function.setNodeCode("BD_" + randomNumber);
			function.setNodeName(board.getName() + "_" + randomNumber);
			function.setXmlTemplate(xmlTemplate);
			function.setModuleId(publishModuleId);
			function.setDivId(function.getNodeCode());
			function.setDivClass(divClass);
			String frontProjectName = request.getContextPath();
			function.setFrontProjectname(frontProjectName.substring(1, frontProjectName.length()));

			// String phantomUrl = persistService.getPhantomUrl(id, userId);
			// http://localhost:8180/cboard/render.html?sid=d9e0ba53c2594c459e113e7bb29c877b#?id=4&pid=1f08fe42cf08431f8412755e609f75fb
			// String[] url = phantomUrl.split("render\\.html");
			// function.setListUrl("render.html" + url[url.length - 1]);
			// http://localhost:8180/icop-bigdata-board/board.html#/dashboard/%E7%A4%BA%E4%BE%8B1/1001
			function.setListUrl("board.html#/dashboard/" + categoryName + "/" + id);

			function.setModify(false);
			function.setPublishType(publishType);
			functions.add(function);
			functionDubboService.saveOrUpdate(functions);
			return new ServiceStatus(ServiceStatus.Status.Success, "success");
		} catch (Exception e) {
			e.printStackTrace();
			return new ServiceStatus(ServiceStatus.Status.Fail, e.getMessage());
		}
	}

	public ViewDashboardBoard getBoardData(Long id) {
		DashboardBoard board = boardDao.getBoard(id);
		JSONObject layout = JSONObject.parseObject(board.getLayout());
		JSONArray rows = layout.getJSONArray("rows");
		for (Object row : rows) {
			JSONObject o = (JSONObject) row;
			if ("param".equals(o.getString("type"))) {
				layout.put("containsParam", true);
				continue;
			}
			JSONArray widgets = o.getJSONArray("widgets");
			for (Object w : widgets) {
				JSONObject ww = (JSONObject) w;
				Long widgetId = ww.getLong("widgetId");
				DashboardWidget widget = widgetDao.getWidget(widgetId);
				JSONObject dataJson = JSONObject.parseObject(widget.getData());
				// DataProviderResult data =
				// dataProviderService.getData(dataJson.getLong("datasource"),
				// Maps.transformValues(dataJson.getJSONObject("query"),
				// Functions.toStringFunction()));
				JSONObject widgetJson = (JSONObject) JSONObject.toJSON(new ViewDashboardWidget(widget));
				// widgetJson.put("queryData", data.getData());
				ww.put("widget", widgetJson);
				ww.put("show", false);
			}
		}
		ViewDashboardBoard view = new ViewDashboardBoard(board);
		view.setLayout(layout);
		return view;
	}

	public ServiceStatus save(String userId, String json) {
		JSONObject jsonObject = JSONObject.parseObject(json);
		DashboardBoard board = new DashboardBoard();
		board.setUserId(userId);
		board.setName(jsonObject.getString("name"));
		board.setCategoryId(jsonObject.getLong("categoryId"));
		board.setLayout(jsonObject.getString("layout"));

		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("user_id", board.getUserId());
		paramMap.put("board_name", board.getName());
		if (boardDao.countExistBoardName(paramMap) <= 0) {
			boardDao.save(board);
			return new ServiceStatus(ServiceStatus.Status.Success, "success", board.getId());
		} else {
			return new ServiceStatus(ServiceStatus.Status.Fail, "Duplicated name");
		}
	}

	public ServiceStatus update(String userId, String json) {
		JSONObject jsonObject = JSONObject.parseObject(json);
		DashboardBoard board = new DashboardBoard();
		board.setUserId(userId);
		board.setName(jsonObject.getString("name"));
		board.setCategoryId(jsonObject.getLong("categoryId"));
		board.setLayout(jsonObject.getString("layout"));
		board.setId(jsonObject.getLong("id"));
		board.setUpdateTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));

		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("board_id", board.getId());
		paramMap.put("user_id", board.getUserId());
		paramMap.put("board_name", board.getName());
		if (boardDao.countExistBoardName(paramMap) <= 0) {
			boardDao.update(board);
			return new ServiceStatus(ServiceStatus.Status.Success, "success");
		} else {
			return new ServiceStatus(ServiceStatus.Status.Fail, "Duplicated name");
		}
	}

	public ServiceStatus delete(String userId, Long id) {
		try {
			boardDao.delete(id, userId);
			return new ServiceStatus(ServiceStatus.Status.Success, "success");
		} catch (Exception e) {
			LOG.error("", e);
			return new ServiceStatus(ServiceStatus.Status.Fail, e.getMessage());
		}
	}

	public byte[] exportBoard(Long id, String userId) {
		PersistContext persistContext = persistService.persist(id, userId);
		List<PersistContext> workbookList = new ArrayList<>();
		workbookList.add(persistContext);
		HSSFWorkbook workbook = xlsProcessService.dashboardToXls(workbookList);
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			workbook.write(outputStream);
			outputStream.close();
			return outputStream.toByteArray();
		} catch (IOException e) {
			LOG.error("", e);
		}
		return null;
	}

}
