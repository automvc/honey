package org.teasoft.honey.osql.core;

import java.util.Map;

import org.teasoft.bee.osql.BeeException;

public class MoreTableHelper {

	static String fieldName2ColumnName(String fieldName, String mainTableAlias, Class<?> mainClass,
			Map<String, MoreTableStruct3> moreTableStructMap) {
		String columnName;
		int dotIndex;
		String tablePart;
		String namePart;
		dotIndex = fieldName.indexOf('.');
		if (dotIndex < 0) {
			columnName = mainTableAlias + "." + _toColumnName(fieldName, mainClass);
		} else {
			tablePart = fieldName.substring(0, dotIndex);
			namePart = fieldName.substring(dotIndex + 1);
			if (tablePart.equals(mainTableAlias)) {
				columnName = tablePart + "." + _toColumnName(namePart, mainClass);
			} else {
				tablePart = _toColumnName(tablePart, null); // fixed 3.0.0; transfer entity name -> table name
				MoreTableStruct3 moreTableStruct = moreTableStructMap.get(tablePart);
				if (moreTableStruct == null) {
					throw new BeeException("Can not found the MoreTableStruct with " + tablePart + " via fieldName: "
							+ fieldName + " , please check the Condition you use!");
//					return null;
				}
				columnName = tablePart + "." + _toColumnName(namePart, moreTableStruct.subClass);
			}
		}
		// fieldName->columnName end

		return columnName;
	}

	private static String _toColumnName(String fieldName, Class entityClass) {
		return HoneyUtil.toColumnName(fieldName, entityClass);
	}

}
