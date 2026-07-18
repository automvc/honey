package org.teasoft.honey.osql.core;

import java.util.List;
import java.util.Map;

public class MoreTableStructOverall {
	// 子表是否是List; 只在第一个子表的MoreTableStruct设置
	List<String> allEntityType;
	// 子表重名的字段； 只在第一个子表的MoreTableStruct设置; 是在解析entity成sql时才设置subDulColumnMap
	Map<String, String> subDulColumnMap;

	private boolean hasAnySubListEntity = false;

	public boolean isHasAnySubListEntity() {
		return hasAnySubListEntity;
	}

	public void setHasAnySubListEntity(boolean isSubListEntity) {
		if (!this.hasAnySubListEntity) this.hasAnySubListEntity = isSubListEntity;
	}

}
