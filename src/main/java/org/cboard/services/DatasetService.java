package org.cboard.services;

import java.sql.Timestamp;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.cboard.dao.DatasetDao;
import org.cboard.pojo.DashboardDataset;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import com.alibaba.fastjson.JSONObject;

/**
 * Created by yfyuan on 2016/10/11.
 */
@Repository
public class DatasetService {

	@Autowired
	private DatasetDao datasetDao;

	public ServiceStatus save(String userId, String json) {
		JSONObject jsonObject = JSONObject.parseObject(json);
		DashboardDataset dataset = new DashboardDataset();
		dataset.setUserId(userId);
		dataset.setName(jsonObject.getString("name"));
		dataset.setData(jsonObject.getString("data"));
		dataset.setCategoryName(jsonObject.getString("categoryName"));
		if (StringUtils.isEmpty(dataset.getCategoryName())) {
			dataset.setCategoryName("默认分类");
		}
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("dataset_name", dataset.getName());
		paramMap.put("user_id", dataset.getUserId());
		paramMap.put("category_name", dataset.getCategoryName());
		if (datasetDao.countExistDatasetName(paramMap) <= 0) {
			datasetDao.save(dataset);
			List<DashboardDataset> datasetList = datasetDao.getDatasetList(userId);
			Long id = 0L;
			if (null != datasetList && datasetList.size() > 0) {
				for (DashboardDataset ds : datasetList) {
					if (ds.getId() > id) {
						id = ds.getId();
					}
				}
			}
			return new ServiceStatus(ServiceStatus.Status.Success, "success", id);
		} else {
			return new ServiceStatus(ServiceStatus.Status.Fail, "Duplicated name");
		}
	}

	public ServiceStatus update(String userId, String json) {
		JSONObject jsonObject = JSONObject.parseObject(json);
		DashboardDataset dataset = new DashboardDataset();
		dataset.setUserId(userId);
		dataset.setId(jsonObject.getLong("id"));
		dataset.setName(jsonObject.getString("name"));
		dataset.setCategoryName(jsonObject.getString("categoryName"));
		dataset.setData(jsonObject.getString("data"));
		dataset.setUpdateTime(new Timestamp(Calendar.getInstance().getTimeInMillis()));
		if (StringUtils.isEmpty(dataset.getCategoryName())) {
			dataset.setCategoryName("默认分类");
		}
		Map<String, Object> paramMap = new HashMap<String, Object>();
		paramMap.put("dataset_name", dataset.getName());
		paramMap.put("user_id", dataset.getUserId());
		paramMap.put("dataset_id", dataset.getId());
		paramMap.put("category_name", dataset.getCategoryName());
		if (datasetDao.countExistDatasetName(paramMap) <= 0) {
			datasetDao.update(dataset);
			return new ServiceStatus(ServiceStatus.Status.Success, "success");
		} else {
			return new ServiceStatus(ServiceStatus.Status.Fail, "Duplicated name");
		}
	}

	public ServiceStatus delete(String userId, Long id) {
		datasetDao.delete(id, userId);
		return new ServiceStatus(ServiceStatus.Status.Success, "success");
	}
}
