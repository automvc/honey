package org.honey.osql.atuogen;

public class GenConfig {
	private String encode = "UTF-8"; // 字符编码

	private String baseDir = "";
	private String packagePath = "";
	private String dbName = ""; // 数据库类型 mysql oracle等
	private String driverName = ""; // 数据库驱动名
	private String url = ""; // 数据库连接地址
	private String username = "";
	private String password = "";
	private String queryTableSql = ""; // 查询所有表名的SQL语句，mysqll,oracle和sql server不用设置
	private boolean genToString;
	private boolean genSerializable;

	public String getEncode() {
		return encode;
	}

	public void setEncode(String encode) {
		this.encode = encode;
	}

	public String getBaseDir() {
		return baseDir;
	}

	public void setBaseDir(String baseDir) {
		this.baseDir = baseDir;
	}

	public String getDriverName() {
		return driverName;
	}

	public void setDriverName(String driverName) {
		this.driverName = driverName;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getQueryTableSql() {
		return queryTableSql;
	}

	public void setQueryTableSql(String queryTableSql) {
		this.queryTableSql = queryTableSql;
	}

	public String getDbName() {
		return dbName;
	}

	public void setDbName(String dbName) {
		this.dbName = dbName;
	}

	public String getPackagePath() {
		return packagePath;
	}

	public void setPackagePath(String packagePath) {
		this.packagePath = packagePath;
	}

	public boolean isGenToString() {
		return genToString;
	}

	public void setGenToString(boolean genToString) {
		this.genToString = genToString;
	}

	public boolean isGenSerializable() {
		return genSerializable;
	}

	public void setGenSerializable(boolean genSerializable) {
		this.genSerializable = genSerializable;
	}

}
