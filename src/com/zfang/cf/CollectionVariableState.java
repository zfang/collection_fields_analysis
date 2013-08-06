package com.zfang.cf;

import java.util.ArrayList;
import java.util.EnumSet;

public enum CollectionVariableState {
   IMMUTABLE, ALIASED, EXTERNAL, UNKNOWN, NULL, NONALIASED, NOINFO;

   public static CollectionVariableState getNewValue(CollectionVariableState oldVal, 
         CollectionVariableState newVal) {
      if (null == oldVal && null == newVal)
         return null;
      if (null == oldVal)
         return newVal;
      if (null == newVal)
         return oldVal;
      if ((oldVal == NULL && newVal == NONALIASED)
            || (newVal == NULL && oldVal == NONALIASED))
         return newVal;

      return oldVal.ordinal() <= newVal.ordinal() ? oldVal : newVal;
   }

   public static CollectionVariableState lastValue() {
      return CollectionVariableState.values()[CollectionVariableState.values().length-1];
   }

}
