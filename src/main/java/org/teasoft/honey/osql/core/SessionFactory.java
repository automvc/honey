package org.teasoft.honey.osql.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.teasoft.bee.osql.exception.NoConfigException;
import org.teasoft.bee.osql.transaction.Transaction;
import org.teasoft.honey.osql.constant.DbConfigConst;
import org.teasoft.honey.osql.transaction.JdbcTransaction;

/**
 * @author Kingstar
 * @since  1.0
 */
public final class SessionFactory {

	private static BeeFactory beeFactory = null;

	public static BeeFactory getBeeFactory() {
		if(beeFactory==null) {
//			beeFactory = new BeeFactory();
			beeFactory = BeeFactory.getInstance();
		}
		return beeFactory;
	}

	public void setBeeFactory(BeeFactory beeFactory) {
		this.beeFactory = beeFactory;
	}

	
	public SessionFactory(){
	}
	public static Connection getConnection() {
		Connection conn = null;
		try {
			DataSource ds=getBeeFactory().getDataSource();
			if (ds != null) { 
				conn = ds.getConnection();
			} else {//do not set the dataSource
				conn = getOriginalConn();
			}
		} 
		catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
       catch (ClassNotFoundException e) {
			Logger.error("Can not find the Database driver!  " + e.getMessage());
			throw new NoConfigException("Can not find the Database driver(maybe miss the jar file).");
		} 
		catch (Exception e) {
			Logger.error("Have Exception when getConnection: "+e.getMessage());
			throw ExceptionHelper.convert(e);
		}

		return conn;
	}

	public static Transaction getTransaction() {
		Transaction tran = null;
		if (getBeeFactory().getTransaction() == null) { // do not set the dataSource
			tran = new JdbcTransaction();
		} else {
			tran = getBeeFactory().getTransaction();
		}

		return tran;
	}

	private static Connection getOriginalConn() throws ClassNotFoundException,SQLException{

		String driverName = HoneyConfig.getHoneyConfig().getDriverName();
		String url = HoneyConfig.getHoneyConfig().getUrl();
		String username = HoneyConfig.getHoneyConfig().getUsername();
		String password = HoneyConfig.getHoneyConfig().getPassword();

		String nullInfo = "";
		if (driverName == null) nullInfo += DbConfigConst.DB_DRIVERNAME + " do not config; ";
		if (url == null) nullInfo += DbConfigConst.DB_URL + " do not config; ";
		if (username == null) nullInfo += DbConfigConst.DB_USERNAM + " do not config; ";
		if (password == null) nullInfo += DbConfigConst.DB_PASSWORD + " do not config; ";

		if (!"".equals(nullInfo)){
			throw new NoConfigException("NoConfigException,Do not set the database info: "+nullInfo);
		}

		Connection conn = null;
//		try {
			Class.forName(driverName);
			conn = DriverManager.getConnection(url, username, password);
			
//		} catch (ClassNotFoundException e) {
//			Logger.error("Can not find the Database driver!  "+e.getMessage());
//			throw new NoConfigException("Can not find the Database driver(maybe miss the config info).");
//		} 
//		catch (SQLException e) {
//			throw ExceptionHelper.convert(e);
//		}

		return conn;
	}
}
