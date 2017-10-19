/**
 * 
 */
package org.cboard.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.cboard.dao.WidgetDao;
import org.cboard.dataprovider.config.AggConfig;
import org.cboard.dataprovider.result.AggregateResult;
import org.cboard.dataprovider.result.ColumnIndex;
import org.cboard.dto.ViewAggConfig;
import org.cboard.pojo.DashboardWidget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Functions;
import com.google.common.collect.Maps;

/**
 * @author wangFeng
 *
 */
@Repository
public class QueryDataService {

	@Autowired
	private WidgetDao widgetDao;

	@Autowired
	private DataProviderService dataProviderService;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public JSONArray getQueryData(Map<String, Object> params) {
		// 获取图表的id
		Long widgetId = Long.valueOf(String.valueOf(params.get("widgetId")));
		DashboardWidget widget = widgetDao.getWidget(widgetId);
		// 获取图表的参数配置信息
		JSONObject data = JSONObject.parseObject(widget.getData());
		// 获取图表的数据源id
		String str1 = String.valueOf(data.get("datasource"));
		Long datasourceId = NumberUtils.isNumber(str1) ? Long.valueOf(str1) : null;
		// 获取图表的查询语句
		String str3 = String.valueOf(data.get("query"));
		String query = "null".equals(str3) || StringUtils.isEmpty(str3) ? null : str3;
		// 获取图表的数据集
		String str2 = String.valueOf(data.get("datasetId"));
		Long datasetId = NumberUtils.isNumber(str2) ? Long.valueOf(str2) : null;

		Boolean reload = false;

		JSONObject widgetConfig = JSONObject.parseObject(String.valueOf(data.get("config")));
		JSONObject con = new JSONObject();
		con.put("rows", getDimensionConfig(String.valueOf(widgetConfig.get("keys"))));
		con.put("columns", getDimensionConfig(String.valueOf(widgetConfig.get("groups"))));
		con.put("values", getValuesConfig(String.valueOf(widgetConfig.get("values"))));
		// 获取前端传的过滤信息
		List<HashMap> list = (ArrayList<HashMap>) params.get("filters");
		// 图表配置的过滤信息
		JSONArray filters = getDimensionConfig(String.valueOf(widgetConfig.get("filters")));
		// 将传过来的过滤条件参数加入到filters
		filters.addAll(genFiltersParams(list));
		con.put("filters", filters);

		String cfg = JSONObject.toJSONString(con);

		Map<String, String> strParams = null;
		if (query != null) {
			JSONObject queryO = JSONObject.parseObject(query);
			strParams = Maps.transformValues(queryO, Functions.toStringFunction());
		}
		AggConfig config = ViewAggConfig.getAggConfig(JSONObject.parseObject(cfg, ViewAggConfig.class));
		AggregateResult aggData = dataProviderService.queryAggData(datasourceId, strParams, datasetId, config, reload);
		return genResultData(aggData);
	}

	@SuppressWarnings("rawtypes")
	private JSONArray genFiltersParams(List<HashMap> list) {
		JSONArray filtersParam = new JSONArray();
		if (null != list && !list.isEmpty()) {
			list.stream().forEach(e -> {
				JSONObject o = new JSONObject();
				o.put("columnName", e.get("col"));
				o.put("filterType", e.get("type"));
				o.put("values", e.get("values"));
				filtersParam.add(o);
			});
		}
		return filtersParam;
	}

	private JSONArray genResultData(AggregateResult aggData) {
		JSONArray result = new JSONArray();
		List<ColumnIndex> columnList = aggData.getColumnList();
		String[][] data = aggData.getData();
		if (null != data && data.length > 0) {
			for (int i = 0; i < data.length; i++) {
				JSONObject o = new JSONObject();
				String[] strings = data[i];
				for (int j = 0; j < strings.length; j++) {
					ColumnIndex column = columnList.get(j);
					o.put(StringUtils.isEmpty(column.getAggType()) ? column.getName()
							: column.getAggType().toUpperCase() + "(" + column.getName() + ")", strings[j]);
				}
				result.add(o);
			}
		} else {
			for (int i = 0; i < columnList.size(); i++) {
				JSONObject o = new JSONObject();
				ColumnIndex column = columnList.get(i);
				o.put(StringUtils.isEmpty(column.getAggType()) ? column.getName()
						: column.getAggType().toUpperCase() + "(" + column.getName() + ")", null);
				result.add(o);
			}
		}
		return result;
	}

	@SuppressWarnings("rawtypes")
	private JSONArray getDimensionConfig(String str) {
		JSONArray result = new JSONArray();
		JSONArray jsonArray = JSONObject.parseArray(str);
		if (null != jsonArray && jsonArray.size() > 0) {
			for (int i = 0; i < jsonArray.size(); i++) {
				Map map = (Map) JSONObject.parse(String.valueOf(jsonArray.get(i)));
				if ("null".equals(String.valueOf(map.get("group")))) {
					JSONObject o = new JSONObject();
					o.put("columnName", map.get("col"));
					o.put("filterType", map.get("type"));
					o.put("values", map.get("values"));
					o.put("id", map.get("id"));
					result.add(o);
				} else {
					JSONArray filters = JSONObject.parseArray(String.valueOf(map.get("filters")));
					if (null != filters && filters.size() > 0) {
						for (int j = 0; j < filters.size(); j++) {
							Map filter = (Map) JSONObject.parse(String.valueOf(filters.get(j)));
							JSONObject o = new JSONObject();
							o.put("columnName", filter.get("col"));
							o.put("filterType", filter.get("type"));
							o.put("values", filter.get("values"));
							result.add(o);
						}
					}
				}
			}
		} else {
			result = jsonArray;
		}

		return result;
	}

	@SuppressWarnings("rawtypes")
	private JSONArray getValuesConfig(String str) {
		JSONArray jsonArray = JSONObject.parseArray(str);
		JSONArray result = new JSONArray();
		if (null != jsonArray && jsonArray.size() > 0) {
			for (int i = 0; i < jsonArray.size(); i++) {
				Map map = (Map) JSONObject.parse(String.valueOf(jsonArray.get(i)));
				JSONArray values = JSONObject.parseArray(String.valueOf(map.get("cols")));
				if (null != values && values.size() > 0) {
					for (int j = 0; j < values.size(); j++) {
						Map value = (Map) JSONObject.parse(String.valueOf(values.get(j)));
						JSONObject o = new JSONObject();
						o.put("column", value.get("col"));
						o.put("aggType", value.get("aggregate_type"));
						if (!result.contains(o)) {
							result.add(o);
						}
					}
				}
			}
		} else {
			jsonArray = result;
		}
		return result;
	}

}
