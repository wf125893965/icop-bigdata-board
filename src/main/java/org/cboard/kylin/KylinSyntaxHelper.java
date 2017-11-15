package org.cboard.kylin;

import java.util.Arrays;
import java.util.StringJoiner;

import org.apache.commons.lang.StringUtils;
import org.cboard.dataprovider.config.DimensionConfig;
import org.cboard.dataprovider.config.ValueConfig;
import org.cboard.dataprovider.util.SqlSyntaxHelper;

/**
 * Created by zyong on 2017/9/18.
 */
public class KylinSyntaxHelper extends SqlSyntaxHelper {
	
	private static final String QUOTATAION = "\"";
	
    private KylinModel kylinModel;

    public KylinSyntaxHelper(KylinModel kylinModel) {
        this.kylinModel = kylinModel;
    }

    @Override
    public String getDimMemberStr(DimensionConfig config, int index) {
        if (kylinModel.getColumnType(config.getColumnName()).startsWith("varchar")) {
            return "'" + config.getValues().get(index) + "'";
        } else {
            return config.getValues().get(index);
        }
    }

    @Override
    public String getAggStr(ValueConfig vConfig) {
        switch (vConfig.getAggType()) {

        case "sum":
			return "SUM(" + surroundWithQutaAll(vConfig.getColumn()) + ") AS sum_"
					+ StringUtils.substringAfter(vConfig.getColumn(), ".");
		case "avg":
			return "AVG(" + surroundWithQutaAll(vConfig.getColumn()) + ") AS avg_"
					+ StringUtils.substringAfter(vConfig.getColumn(), ".");
		case "max":
			return "MAX(" + surroundWithQutaAll(vConfig.getColumn()) + ") AS max_"
					+ StringUtils.substringAfter(vConfig.getColumn(), ".");
		case "min":
			return "MIN(" + surroundWithQutaAll(vConfig.getColumn()) + ") AS min_"
					+ StringUtils.substringAfter(vConfig.getColumn(), ".");
		case "distinct":
			return "COUNT(DISTINCT " + surroundWithQutaAll(vConfig.getColumn()) + ") AS count_d_"
					+ StringUtils.substringAfter(vConfig.getColumn(), ".");
		default:
			return "COUNT(" + surroundWithQutaAll(vConfig.getColumn()) + ") AS count_"
					+ StringUtils.substringAfter(vConfig.getColumn(), ".");
		}
    }
    private String surroundWithQutaAll(String text) {
		String table = StringUtils.substringBefore(text, ".");
		String column = StringUtils.substringAfter(text, ".");
		return table + "." + surroundWithQuta(column);
	}
    private String surroundWithQuta(String text) {
		return QUOTATAION + text + QUOTATAION;
	}
    private String formatTableName(String rawName) {
		String tmp = rawName.replaceAll("\"", "");
		StringJoiner joiner = new StringJoiner(".");
		Arrays.stream(tmp.split("\\.")).map(i -> surroundWithQuta(i)).forEach(joiner::add);
		return joiner.toString();
	}
    



}
