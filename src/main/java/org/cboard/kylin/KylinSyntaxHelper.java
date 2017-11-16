package org.cboard.kylin;

import org.apache.commons.lang.StringUtils;
import org.cboard.dataprovider.config.DimensionConfig;
import org.cboard.dataprovider.config.ValueConfig;
import org.cboard.dataprovider.util.SqlSyntaxHelper;

/**
 * Created by zyong on 2017/9/18.
 */
public class KylinSyntaxHelper extends SqlSyntaxHelper {
	
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
}
