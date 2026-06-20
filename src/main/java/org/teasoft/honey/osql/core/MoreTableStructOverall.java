package org.teasoft.honey.osql.core;

import java.util.List;
import java.util.Map;

public class MoreTableStructOverall {
	List<String> allEntityType; // 子表是否是List; 只在第一个子表的MoreTableStruct设置
	Map<String, String> subDulColumnMap; // 子表重名的字段； 只在第一个子表的MoreTableStruct设置

	private boolean hasAnySubListEntity = false;

	public boolean isHasAnySubListEntity() {
		return hasAnySubListEntity;
	}

	public void setHasAnySubListEntity(boolean isSubListEntity) {
		if (!this.hasAnySubListEntity)
			this.hasAnySubListEntity = isSubListEntity;
	}

}
