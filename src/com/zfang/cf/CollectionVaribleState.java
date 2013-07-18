package com.zfang.cf;

import java.util.EnumSet;
import java.util.Set;

public enum CollectionVaribleState {
   ALIASED, EXTERNAL, UNKNOWN, NONALIASED;

   public static final Set<CollectionVaribleState> allStates = EnumSet.allOf(CollectionVaribleState.class);
   public static CollectionVaribleState getNewValue(CollectionVaribleState oldVal, 
         CollectionVaribleState newVal) {
      return oldVal.ordinal() <= newVal.ordinal() ? oldVal : newVal;
   }
}

