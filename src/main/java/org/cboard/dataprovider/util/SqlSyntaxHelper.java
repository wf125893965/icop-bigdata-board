package org.cboard.dataprovider.util;

import java.sql.Types;
import java.util.Arrays;
import java.util.Map;
import java.util.StringJoiner;

import org.cboard.dataprovider.config.DimensionConfig;
import org.cboard.dataprovider.config.ValueConfig;

/**
 * Created by zyong on 2017/9/18.
 */
public class SqlSyntaxHelper {

    private Map<String, Integer> columnTypes;

    public String getProjectStr(DimensionConfig config) {
        return config.getColumnName();
    }

    public String getDimMemberStr(DimensionConfig config, int index) {
        switch (columnTypes.get(config.getColumnName().toUpperCase())) {
            case Types.VARCHAR:
            case Types.CHAR:
            case Types.NVARCHAR:
            case Types.NCHAR:
            case Types.CLOB:
            case Types.NCLOB:
            case Types.LONGVARCHAR:
            case Types.LONGNVARCHAR:
            case Types.DATE:
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return "'" + config.getValues().get(index) + "'";
            default:
                return config.getValues().get(index);
        }
    }

    public String getAggStr(ValueConfig vConfig) {
        String aggExp = vConfig.getColumn();
        switch (vConfig.getAggType()) {
            case "sum":
                return "SUM(" + aggExp + ")";
            case "avg":
                return "AVG(" + aggExp + ")";
            case "max":
                return "MAX(" + aggExp + ")";
            case "min":
                return "MIN(" + aggExp + ")";
            case "distinct":
                return "COUNT(DISTINCT " + aggExp + ")";
            default:
                return "COUNT(" + aggExp + ")";
        }
    }

    public SqlSyntaxHelper setColumnTypes(Map<String, Integer> columnTypes) {
        this.columnTypes = columnTypes;
        return this;
    }

    public String formatTableName(String rawName) {
  		String tmp = rawName.replaceAll("\"", "");
  		StringJoiner joiner = new StringJoiner(".");
  		Arrays.stream(tmp.split("\\.")).forEach(joiner::add);
  		return joiner.toString();
  	}
}