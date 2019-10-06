package org.teasoft.honey.osql.core;

import java.util.List;

/**
 * @author Kingstar
 * @since 1.0
 */
class SqlValueWrap {
	private String sql;
	private List<PreparedValue> list;
	private StringBuffer valueBuffer;
	
	private String tableNames; //v1.4  2019-09-29

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}

	public List<PreparedValue> getList() {
		return list;
	}

	public void setList(List<PreparedValue> list) {
		this.list = list;
	}

	public StringBuffer getValueBuffer() {
		return valueBuffer;
	}

	public void setValueBuffer(StringBuffer valueBuffer) {
		this.valueBuffer = valueBuffer;
	}

	public String getTableNames() {
		return tableNames;
	}

	public void setTableNames(String tableNames) {
		this.tableNames = tableNames;
	}
	
}
