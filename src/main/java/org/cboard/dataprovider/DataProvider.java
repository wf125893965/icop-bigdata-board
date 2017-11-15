package org.cboard.dataprovider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.cboard.dataprovider.aggregator.Aggregatable;
import org.cboard.dataprovider.aggregator.InnerAggregator;
import org.cboard.dataprovider.config.AggConfig;
import org.cboard.dataprovider.config.CompositeConfig;
import org.cboard.dataprovider.config.ConfigComponent;
import org.cboard.dataprovider.config.DimensionConfig;
import org.cboard.dataprovider.expression.NowFunction;
import org.cboard.dataprovider.result.AggregateResult;
import org.cboard.pojo.DashboardRole;
import org.cboard.services.AuthenticationService;
import org.cboard.services.RoleService;
import org.cboard.util.CookiesUtil;
import org.cboard.util.NaturalOrderComparator;
import org.cboard.util.SetTenantidUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.alibaba.fastjson.JSONObject;
import com.google.common.base.Charsets;
import com.google.common.hash.Hashing;
import com.googlecode.aviator.AviatorEvaluator;
import com.yyjz.icop.orgcenter.company.service.ICompanyService;
import com.yyjz.icop.orgcenter.company.vo.CompanyVO;

/**
 * Created by zyong on 2017/1/9.
 */
public abstract class DataProvider {

	@Autowired
	private AuthenticationService authenticationService;
	@Autowired
	private RoleService roleService;
	@Autowired
	private ICompanyService iCompanyService;
	private InnerAggregator innerAggregator;
	protected Map<String, String> dataSource;
	protected Map<String, String> query;
	private int resultLimit;
	private long interval = 12 * 60 * 60; // second

	@Autowired
	private HttpServletRequest request;

	public static final String NULL_STRING = "#NULL";
	private static final Logger logger = LoggerFactory.getLogger(DataProvider.class);

	static {
		AviatorEvaluator.addFunction(new NowFunction());
	}

	public abstract boolean doAggregationInDataSource();

	/**
	 * get the aggregated data by user's widget designer
	 *
	 * @return
	 */
	public final AggregateResult getAggData(AggConfig ac, boolean reload) throws Exception {
		evalValueExpression(ac);
		if (this instanceof Aggregatable && doAggregationInDataSource()) {
			return ((Aggregatable) this).queryAggData(ac);
		} else {
			checkAndLoad(reload);
			return innerAggregator.queryAggData(ac);
		}
	}

	public final String getViewAggDataQuery(AggConfig config) throws Exception {
		evalValueExpression(config);
		if (this instanceof Aggregatable && doAggregationInDataSource()) {
			return ((Aggregatable) this).viewAggDataQuery(config);
		} else {
			return "Not Support";
		}
	}

	/**
	 * Get the options values of a dimension column
	 *
	 * @param columnName
	 * @return
	 */
	public final String[] getDimVals(String columnName, AggConfig config, boolean reload) throws Exception {
		String[] dimVals = null;
		evalValueExpression(config);
		if (this instanceof Aggregatable && doAggregationInDataSource()) {
			dimVals = ((Aggregatable) this).queryDimVals(columnName, config);
		} else {
			checkAndLoad(reload);
			dimVals = innerAggregator.queryDimVals(columnName, config);
		}
		return Arrays.stream(dimVals).map(member -> {
			return Objects.isNull(member) ? NULL_STRING : member;
		}).sorted(new NaturalOrderComparator()).limit(1000).toArray(String[]::new);
	}

	public final String[] getColumn(boolean reload) throws Exception {
		String[] columns = null;
		if (this instanceof Aggregatable && doAggregationInDataSource()) {
			columns = ((Aggregatable) this).getColumn();
		} else {
			checkAndLoad(reload);
			columns = innerAggregator.getColumn();
		}
		Arrays.sort(columns);
		return columns;
	}

	private void checkAndLoad(boolean reload) throws Exception {
		String key = getLockKey(dataSource, query);
		synchronized (key.intern()) {
			if (reload || !innerAggregator.checkExist()) {
				String[][] data = getData();
				innerAggregator.loadData(data, interval);
				logger.info("loadData {}", key);
			}
		}
	}

	private void evalValueExpression(AggConfig ac) {
		if (ac == null) {
			return;
		}
		ac.getFilters().forEach(e -> evaluator(e));
		ac.getColumns().forEach(e -> evaluator(e));
		ac.getRows().forEach(e -> evaluator(e));
	}

	private void evaluator(ConfigComponent e) {
		if (e instanceof DimensionConfig) {
			DimensionConfig dc = (DimensionConfig) e;
			dc.setValues(dc.getValues().stream().flatMap(v -> getFilterValue(v)).collect(Collectors.toList()));
		}
		if (e instanceof CompositeConfig) {
			CompositeConfig cc = (CompositeConfig) e;
			cc.getConfigComponents().forEach(_e -> evaluator(_e));
		}
	}

	private Stream<String> getFilterValue(String value) {
		List<String> list = new ArrayList<>();
		if (value == null || !(value.startsWith("{") && value.endsWith("}"))) {
			list.add(value);
		} else if ("{loginName}".equals(value)) {
			list.add(authenticationService.getCurrentUser().getUsername());
		} else if ("{userName}".equals(value)) {
			list.add(authenticationService.getCurrentUser().getName());
		} else if ("{userRoles}".equals(value)) {
			List<DashboardRole> roles = roleService.getCurrentRoleList();
			roles.forEach(role -> list.add(role.getRoleName()));
		} else if ("{curOrg}".equals(value)) {
			Cookie cookie = CookiesUtil.getCookieByName(request, "companyId");
			if (null != cookie) {
				list.add(cookie.getValue());
			}
		} else if ("{subOrg}".equals(value)) {
			Cookie cookie = CookiesUtil.getCookieByName(request, "companyId");
			if (null != cookie) {
				String companyId = cookie.getValue();
				try {
					SetTenantidUtil.setTenantid(request);
				} catch (Exception e) {
					e.printStackTrace();
				}
				List<CompanyVO> CompanyVOLst = iCompanyService.getChildrenCompanyById(companyId);
				for (CompanyVO vo : CompanyVOLst) {
					if (!StringUtils.isEmpty(vo.getId()))
						list.add(vo.getId());
				}
				if (list.size() == 0) {
					list.add(companyId);
				}
			}
		} else {
			list.add(AviatorEvaluator.compile(value.substring(1, value.length() - 1), true).execute().toString());
		}
		return list.stream();
	}

	private String getLockKey(Map<String, String> dataSource, Map<String, String> query) {
		return Hashing.md5().newHasher()
				.putString(JSONObject.toJSON(dataSource).toString() + JSONObject.toJSON(query).toString(),
						Charsets.UTF_8)
				.hash().toString();
	}

	public List<DimensionConfig> filterCCList2DCList(List<ConfigComponent> filters) {
		List<DimensionConfig> result = new LinkedList<>();
		filters.stream().forEach(cc -> {
			result.addAll(configComp2DimConfigList(cc));
		});
		return result;
	}

	public List<DimensionConfig> configComp2DimConfigList(ConfigComponent cc) {
		List<DimensionConfig> result = new LinkedList<>();
		if (cc instanceof DimensionConfig) {
			result.add((DimensionConfig) cc);
		} else {
			Iterator<ConfigComponent> iterator = cc.getIterator();
			while (iterator.hasNext()) {
				ConfigComponent next = iterator.next();
				result.addAll(configComp2DimConfigList(next));
			}
		}
		return result;
	}

	abstract public String[][] getData() throws Exception;

	public void setDataSource(Map<String, String> dataSource) {
		this.dataSource = dataSource;
	}

	public void setQuery(Map<String, String> query) {
		this.query = query;
	}

	public void setResultLimit(int resultLimit) {
		this.resultLimit = resultLimit;
	}

	public int getResultLimit() {
		return resultLimit;
	}

	public void setInterval(long interval) {
		this.interval = interval;
	}

	public void setInnerAggregator(InnerAggregator innerAggregator) {
		this.innerAggregator = innerAggregator;
	}

}
