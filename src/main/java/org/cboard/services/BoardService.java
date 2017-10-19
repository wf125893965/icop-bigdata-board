package org.cboard.services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.cboard.dao.BoardDao;
import org.cboard.dao.WidgetDao;
import org.cboard.dto.ViewDashboardBoard;
import org.cboard.dto.ViewDashboardWidget;
import org.cboard.pojo.DashboardBoard;
import org.cboard.pojo.DashboardWidget;
import org.cboard.services.persist.PersistContext;
import org.cboard.services.persist.excel.XlsProcessService;
import org.cboard.util.SystemUtil;
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

	@Autowired
	private BoardDao boardDao;

	@Autowired
	private WidgetDao widgetDao;

	@Autowired
	private PersistService persistService;

	@Autowired
	private XlsProcessService xlsProcessService;

	@Autowired
	private FunctionDubboService functionDubboService;

	@Autowired
	private HttpServletRequest request;

	@Value("${module.id.report}")
	private String publishModuleId;

	private String divClass = "patch-material";

	private String xmlTemplate = "base";

	public List<DashboardBoard> getBoardList(String userId) {
		return boardDao.getBoardList(userId);
	}

	public ViewDashboardBoard getBoardData(Long id) {
		DashboardBoard board = boardDao.getBoard(id);
		JSONObject layout = JSONObject.parseObject(board.getLayout());
		JSONArray rows = layout.getJSONArray("rows");
		for (Object row : rows) {
			JSONObject o = (JSONObject) row;
			if ("param".equals(o.getString("type"))) {
				continue;
			}
			JSONArray widgets = o.getJSONArray("widgets");
			for (Object w : widgets) {
				JSONObject ww = (JSONObject) w;
				Long widgetId = ww.getLong("widgetId");
				DashboardWidget widget = widgetDao.getWidget(widgetId);
				// JSONObject dataJson =
				// JSONObject.parseObject(widget.getData());
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
			e.printStackTrace();
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
			e.printStackTrace();
		}
		return null;
	}

	public ServiceStatus publishBoard(String userId, Long id) {
		try {
			DashboardBoard board = boardDao.getBoard(id);

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
			function.setListUrl("dashboard/viewBoard.do?id=" + id);
			function.setModify(false);
			functions.add(function);
			functionDubboService.saveOrUpdate(functions);
			return new ServiceStatus(ServiceStatus.Status.Success, "success");
		} catch (Exception e) {
			e.printStackTrace();
			return new ServiceStatus(ServiceStatus.Status.Fail, e.getMessage());
		}
	}

}
