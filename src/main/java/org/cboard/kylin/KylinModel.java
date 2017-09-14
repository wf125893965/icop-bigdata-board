package org.cboard.kylin;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.cboard.exception.CBoardException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthorizationInterceptor;
import org.springframework.web.client.RestTemplate;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

class KylinModel implements Serializable {
	private static final long serialVersionUID = -6827606497416189044L;
	private JSONObject model;
	private Map<String, String> columnTable = new HashMap<String,String>();
	private Map<String, String> tableAlias = new HashMap<String,String>();
	private Map<String, String> columnType = new HashMap<String,String>();
	private static final String QUOTATAION = "\"";

	/*public String getColumnAndAlias(String column) {
		return tableAlias.get(columnTable.get(column)) + "." + surroundWithQuta(column);
	}*/

	public String getTable(String column) {
		return columnTable.get(column);
	}

	/*public String getTableAlias(String table) {
		return tableAlias.get(table);
	}*/

	private Map<String, String> getColumnsType(String table, String serverIp, String username, String password) {
		RestTemplate restTemplate = new RestTemplate();
		restTemplate.getInterceptors().add(new BasicAuthorizationInterceptor(username, password));
		ResponseEntity<String> a = restTemplate.getForEntity("http://" + serverIp + "/kylin/api/tables/{tableName}",
				String.class, table);
		JSONObject jsonObject = JSONObject.parseObject(a.getBody());
		Map<String, String> result = new HashMap<String,String>();
		JSONArray jsonArray = jsonObject.getJSONArray("columns");
		for(Object e:jsonArray){
			JSONObject je = (JSONObject)e;
			result.put(je.getString("name"), je.getString("datatype"));
		}
		/*jsonObject.getJSONArray("columns").stream().map(e -> (JSONObject) e)
				.forEach(e -> result.put(e.getString("name"), e.getString("datatype")));*/
		return result;
	}

	public String getColumnType(String column) {
		return columnType.get(column);
	}
	
	
	
	public KylinModel(JSONObject model,String serverIp, String username, String password) throws Exception {
		if (model == null) {
			throw new CBoardException("Model not found");
		}
		this.model = model;
		Map<String,String> tableMap = new HashMap<String,String>();
		
		String factTable = model.getString("fact_table");
		tableMap.put(StringUtils.substringAfter(factTable, "."), factTable);
		JSONArray lookupsJArray = model.getJSONArray("lookups");
		for (Object e : lookupsJArray) {
			tableMap.put(((JSONObject)e).getString("alias"),((JSONObject)e).getString("table"));
		}
		JSONArray jSONArray = model.getJSONArray("dimensions");
		
		for (Object e : jSONArray) {
			String talis= ((JSONObject) e).getString("table");
			String t = tableMap.get(talis);
			Map<String, String> types = getColumnsType(t, serverIp, username, password);
			Set<Entry<String, String>> set = types.entrySet();
			for (Entry<String, String> et : set) {
				columnType.put(talis+"."+et.getKey(), et.getValue());
			}
			JSONArray jsonArray = ((JSONObject)e).getJSONArray("columns");
			String table = ((JSONObject)e).getString("table");
			for (Object s : jsonArray) {
				/*String alias = tableAlias.get(t);
				if (alias == null) {
					alias = "_t" + tableAlias.keySet().size() + 1;
					tableAlias.put(t, alias);
				}*/
				
				columnTable.put(table+"."+s.toString(), t);
			}
			
		}
		/*model.getJSONArray("dimensions").forEach(e -> {
			String t = ((JSONObject) e).getString("table");
			Map<String, String> types = getColumnsType(t, serverIp, username, password);
			types.entrySet().forEach(et -> columnType.put(et.getKey(), et.getValue()));
			((JSONObject) e).getJSONArray("columns").stream().map(c -> c.toString()).forEach(s -> {
				String alias = tableAlias.get(t);
				if (alias == null) {
					alias = "_t" + tableAlias.keySet().size() + 1;
					tableAlias.put(t, alias);
				}
				columnTable.put(s, t);
			});
		});*/
		model.getJSONArray("metrics").stream().map(e -> e.toString()).forEach(s -> {
			String t = model.getString("fact_table");
		/*	String alias = tableAlias.get(t);
			if (alias == null) {
				alias = "_t" + tableAlias.keySet().size() + 1;
				tableAlias.put(t, alias);
			}*/
			columnTable.put(s, t);
		});
	}

	public String geModelSql() {
		String factTable = formatTableName(model.getString("fact_table"));
		return String.format("%s %s %s", factTable, StringUtils.substringAfter(model.getString("fact_table"), "."), getJoinSql(tableAlias.get(factTable)));
	}

	private String getJoinSql(String factAlias) {
		/*JSONArray jsonArray = model.getJSONArray("lookups");
		for(Object e:jsonArray){
			JSONObject j = (JSONObject) e;
			JSONArray jsonArrayPK = j.getJSONObject("join").getJSONArray("primary_key");
			for(Object opk:jsonArrayPK){
				
			}
			String[] pk = (String[])jsonArrayPK.toArray();
			JSONArray jsonArrayFK = j.getJSONObject("join").getJSONArray("primary_key");
			String[] fk = (String[])jsonArrayFK.toArray();
			List<String> on = new ArrayList<>();
			for (int i = 0; i < pk.length; i++) {
				on.add(String.format("%s.%s = %s.%s", tableAlias.get(j.getString("table")), surroundWithQuta(pk[i]),
						factAlias, surroundWithQuta(fk[i])));
			}
			String type = j.getJSONObject("join").getString("type").toUpperCase();
			String pTable = formatTableName(j.getString("table"));
			String onStr = on.stream().collect(Collectors.joining(" and "));
			return String.format("\n %s JOIN %s %s ON %s", type, pTable, tableAlias.get(pTable), onStr);
		};*/
		String s = model.getJSONArray("lookups").stream().map(e -> {
			JSONObject j = (JSONObject) e;
			String[] pk = j.getJSONObject("join").getJSONArray("primary_key").stream().map(p -> p.toString())
					.toArray(String[]::new);
			String[] fk = j.getJSONObject("join").getJSONArray("foreign_key").stream().map(p -> p.toString())
					.toArray(String[]::new);
			List<String> on = new ArrayList<>();
			for (int i = 0; i < pk.length; i++) {
				on.add(String.format("%s = %s", surroundWithQutaAll(pk[i]),surroundWithQutaAll(fk[i])));
				/*on.add(String.format("%s.%s = %s.%s", tableAlias.get(j.getString("table")),
                        surroundWithQuta(pk[i]), factAlias, surroundWithQuta(fk[i])));*/
			}
			String type = j.getJSONObject("join").getString("type").toUpperCase();
			String pTable = formatTableName(j.getString("table"));
			String onStr = on.stream().collect(Collectors.joining(" and "));
			return String.format("\n %s JOIN %s %s ON %s", type, pTable, j.getString("alias"), onStr);
		}).collect(Collectors.joining(" "));
		return s;
	}

	public String[] getColumns() {
		List<String> result = new ArrayList<>();
		/*model.getJSONArray("dimensions").forEach(
				e -> ((JSONObject) e).getJSONArray("columns").stream().map(c -> c.toString()).forEach(result::add));*/
		
		JSONArray jsonArray = model.getJSONArray("dimensions");
		for(Object e:jsonArray){
			JSONArray ejsonArray = ((JSONObject) e).getJSONArray("columns");
			String table = ((JSONObject) e).getString("table");
			for(Object c:ejsonArray){
				result.add(table+"."+c.toString());
			}
		}
		
		model.getJSONArray("metrics").stream().map(e -> e.toString()).forEach(result::add);
		return result.toArray(new String[0]);
	}

	public String formatTableName(String rawName) {
		String tmp = rawName.replaceAll("\"", "");
		StringJoiner joiner = new StringJoiner(".");
		Arrays.stream(tmp.split("\\.")).map(i -> surroundWithQuta(i)).forEach(joiner::add);
		return joiner.toString();
	}

	private String surroundWithQuta(String text) {
		return QUOTATAION + text + QUOTATAION;
	}
	private String surroundWithQutaAll(String text) {
		String table = StringUtils.substringBefore(text, ".");
		String column = StringUtils.substringAfter(text, ".");
		return table + "." + surroundWithQuta(column);
	}

}
