package com.zfang.cf;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum CollectionVaribleState {
   ALIASED, EXTERNAL, UNKNOWN, NONALIASED;

   public static final List<CollectionVaribleState> allStates = new ArrayList<CollectionVaribleState>(EnumSet.allOf(CollectionVaribleState.class));

   public static CollectionVaribleState getNewValue(CollectionVaribleState oldVal, 
         CollectionVaribleState newVal) {
      return oldVal.ordinal() <= newVal.ordinal() ? oldVal : newVal;
   }
}
