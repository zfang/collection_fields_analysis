package com.hang.ld;

import soot.SootField;
import soot.jimple.toolkits.pointer.InstanceKey;

public class ObjectFieldPair {

   private InstanceKey object;
   private SootField field;

   public ObjectFieldPair(InstanceKey object, SootField field) {
      this.object = object;
      this.field = field;
   }

	public int hashCode() {
		return 
         (null == object ? 0 : object.hashCode()) 
         ^ 
         (null == field ? 0 : field.hashCode());
	}

   public boolean equals(Object obj) {
      if (null == obj) {
         return false;
      }
      if (this == obj) {
         return true;
      }
      if (! (obj instanceof ObjectFieldPair)) {
         return false;
      }
      return ((ObjectFieldPair)obj).hashCode() == hashCode();
   }

   public InstanceKey getObject() {
      return object;
   }
   
   public SootField getField() {
      return field;
   }

   public String toString() {
      return new StringBuilder()
         .append(null == object ? "null" : object.toString())
         .append(":")
         .append(null == field ? "null" : field.toString())
         .toString();
   }

}
