package org.teasoft.honey.osql.constant;

public interface NullEmpty {
    int NULL=0;
    int EMPTY_STRING=1;
    int NULL_AND_EMPTY_STRING=2;
    
    int EXCLUDE=-1;  //exclude null, empty.null,empty两者都排除
}
