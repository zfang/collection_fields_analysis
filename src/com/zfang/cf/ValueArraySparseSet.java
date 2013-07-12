package com.zfang.cf;

import soot.EquivTo;
import soot.toolkits.scalar.ArraySparseSet;

class ValueArraySparseSet extends ArraySparseSet {
   public boolean contains(Object obj) {
      for (int i = 0; i < numElements; i++)
         if (elements[i] instanceof EquivTo
               && ((EquivTo) elements[i]).equivTo(obj))
            return true;
         else if (elements[i].equals(obj))
            return true;
      return false;
   }
}

