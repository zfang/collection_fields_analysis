package com.zfang.cf;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum CollectionVariableState {
   ALIASED, EXTERNAL, UNKNOWN, NONALIASED, NOINFO;

   public static final List<CollectionVariableState> allStates = 
      new ArrayList<CollectionVariableState>(EnumSet.allOf(CollectionVariableState.class)){{
         remove(CollectionVariableState.NOINFO);
      }};

   public static CollectionVariableState getNewValue(CollectionVariableState oldVal, 
         CollectionVariableState newVal) {
      if (null == oldVal && null == newVal)
         return null;
      if (null == oldVal)
         return newVal;
      if (null == newVal)
         return oldVal;
      return oldVal.ordinal() <= newVal.ordinal() ? oldVal : newVal;
   }

   public static CollectionVariableState lastValue() {
      return allStates.get(allStates.size()-1);
   }
}
