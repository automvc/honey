package org.teasoft.honey.osql.core;

import org.teasoft.bee.osql.BeeSql;
import org.teasoft.bee.osql.CallableSQL;
import org.teasoft.bee.osql.ObjToSQL;
import org.teasoft.bee.osql.ObjToSQLRich;
import org.teasoft.bee.osql.PreparedSQL;
import org.teasoft.bee.osql.Suid;
import org.teasoft.bee.osql.SuidRich;
import org.teasoft.bee.osql.dialect.DbFeature;
import org.teasoft.bee.osql.exception.NoConfigException;
import org.teasoft.honey.osql.constant.DatabaseConst;
import org.teasoft.honey.osql.dialect.mysql.MySqlFeature;
import org.teasoft.honey.osql.dialect.oracle.OracleFeature;
import org.teasoft.honey.osql.dialect.sqlserver.SqlServerFeature;

/**
 * @author Kingstar
 * @since  1.0
 */
public class HoneyFactory {
	
	private Suid suid;
	private SuidRich suidRich;
	private BeeSql beeSql;
	private ObjToSQL objToSQL;
	private ObjToSQLRich objToSQLRich;
	private PreparedSQL preparedSQL;
	private CallableSQL callableSQL;

	public Suid getSuid() {
		if(suid==null) return new ObjSQL();
		else return suid;                      //可以通过配置spring bean的方式注入
	}

	public void setSuid(Suid suid) {
		this.suid = suid;
	}
	
	public SuidRich getSuidRich() {
		if(suidRich==null) return new ObjSQLRich();
		else return suidRich;
	}

	public void setSuidRich(SuidRich suidRich) {
		this.suidRich = suidRich;
	}

	public BeeSql getBeeSql() {
		if(this.beeSql==null) return new SqlLib();
		return beeSql;
	}

	public void setBeeSql(BeeSql beeSql) {
		this.beeSql = beeSql;
	}

	public ObjToSQL getObjToSQL() {
		if(objToSQL==null) return new ObjectToSQL();
		else return objToSQL;
	}

	public void setObjToSQL(ObjToSQL objToSQL) {
		this.objToSQL = objToSQL;
	}

	public ObjToSQLRich getObjToSQLRich() {
		if(objToSQLRich==null) return new ObjectToSQLRich();
		else return objToSQLRich;
	}

	public void setObjToSQLRich(ObjToSQLRich objToSQLRich) {
		this.objToSQLRich = objToSQLRich;
	}

	public PreparedSQL getPreparedSQL() {
		if(preparedSQL==null) return new PreparedSqlLib();
		else return preparedSQL;
	}

	public void setPreparedSQL(PreparedSQL preparedSQL) {
		this.preparedSQL = preparedSQL;
	}

	public CallableSQL getCallableSQL() {
		if(callableSQL==null) return new CallableSqlLib();
		else return callableSQL;
	}

	public void setCallableSQL(CallableSQL callableSQL) {
		this.callableSQL = callableSQL;
	}

	public DbFeature getDbDialect() {
		if (DatabaseConst.MYSQL.equalsIgnoreCase((HoneyContext.getDbDialect())))
			return new MySqlFeature();
		else if (DatabaseConst.ORACLE.equalsIgnoreCase((HoneyContext.getDbDialect())))
			return new OracleFeature();
		else if (DatabaseConst.SQLSERVER.equalsIgnoreCase((HoneyContext.getDbDialect())))
			return new SqlServerFeature();
		else {
			throw new NoConfigException("Error: Do not set the database name. ");
		}
	}
}
