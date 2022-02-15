package org.teasoft.honey.osql.transaction;

import java.sql.Connection;
import java.sql.SQLException;

import org.teasoft.bee.osql.BeeSQLException;
import org.teasoft.bee.osql.transaction.Transaction;
import org.teasoft.bee.osql.transaction.TransactionIsolationLevel;
import org.teasoft.honey.osql.core.ExceptionHelper;
import org.teasoft.honey.osql.core.HoneyContext;
import org.teasoft.honey.osql.core.Logger;
import org.teasoft.honey.osql.core.SessionFactory;

/**
 * @author Kingstar
 * @since  1.0
 */
public class JdbcTransaction implements Transaction {

	private Connection conn = null;
	private boolean oldAutoCommit;
	private boolean isBegin = false;

	private Connection initOneConn() {
		return SessionFactory.getConnection();
	}

	@Override
	public void begin() {
		Logger.info(" JdbcTransaction begin. ");
		try {
			this.conn = initOneConn();

			setOldAutoCommit(conn.getAutoCommit());
			conn.setAutoCommit(false);

			HoneyContext.setCurrentConnection(this.conn); //存入上下文

			isBegin = true;
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	@Override
	public void commit() {
		Logger.info(" JdbcTransaction commit. ");
		if (!isBegin) throw new BeeSQLException("The Transaction did not to begin!");
		try {
			if (conn != null && !conn.getAutoCommit()) {
				conn.commit();
				if (oldAutoCommit != conn.getAutoCommit()) conn.setAutoCommit(oldAutoCommit);
			}
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			_close();
			isBegin = false;
		}
	}

	@Override
	public void rollback() {
		Logger.info(" JdbcTransaction rollback. ");
		try {
			if (conn != null && !conn.getAutoCommit()) {
				conn.rollback();
				if (oldAutoCommit != conn.getAutoCommit()) conn.setAutoCommit(oldAutoCommit);
			}
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		} finally {
			_close();
		}
	}

	@Override
	public void setReadOnly(boolean readOnly) {
		try {
			conn.setReadOnly(readOnly);
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	@Override
	public void setTransactionIsolation(TransactionIsolationLevel transactionIsolationLevel) {
		try {
			conn.setTransactionIsolation(transactionIsolationLevel.getLevel());
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	@Override
	public boolean isReadOnly() {
		try {
			return conn.isReadOnly();
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	@Override
	public int getTransactionIsolation() {
		try {
			return conn.getTransactionIsolation();
		} catch (SQLException e) {
			throw ExceptionHelper.convert(e);
		}
	}

	@Override
	public void setTimeout(int second) {
		//todo
		Logger.error("Donot support setTimeout(int second) in JdbcTransaction");
	}

	private void setOldAutoCommit(boolean oldAutoCommit) {
		this.oldAutoCommit = oldAutoCommit;
	}

	private void _close() {
		if (conn != null) {
			try {
				conn.close();
			} catch (SQLException e) {
				throw ExceptionHelper.convert(e);
			} finally {
				HoneyContext.removeCurrentConnection(); //事务结束时要删除
				
//				boolean enableMultiDs = HoneyConfig.getHoneyConfig().multiDS_enable;
//				int multiDsType = HoneyConfig.getHoneyConfig().multiDS_type;
//				boolean differentDbType=HoneyConfig.getHoneyConfig().multiDS_differentDbType;
////				if (enableMultiDs && multiDsType == 2) {//仅分库,有多个数据源时
//				if (enableMultiDs && (multiDsType ==2 || (multiDsType ==1 && differentDbType))) {

//				if (HoneyContext.isNeedDs()) {//放到拦截器中
//					HoneyContext.removeCurrentRoute(); 
//				}
			}
		}
	}
}
