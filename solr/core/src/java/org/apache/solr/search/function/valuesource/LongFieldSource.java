/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.solr.search.function.valuesource;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.FieldCache;
import org.apache.lucene.util.Bits;
import org.apache.solr.search.function.FuncValues;
import org.apache.solr.search.function.funcvalues.LongDocValues;
import org.apache.solr.search.mutable.MutableValue;
import org.apache.solr.search.mutable.MutableValueLong;

import java.io.IOException;
import java.util.Map;

/**
 * Obtains long field values from {@link FieldCache#getLongs} and makes those
 * values available as other numeric types, casting as needed.
 */
public class LongFieldSource extends FieldCacheSource {

  protected final FieldCache.LongParser parser;

  public LongFieldSource(String field) {
    this(field, null);
  }

  public LongFieldSource(String field, FieldCache.LongParser parser) {
    super(field);
    this.parser = parser;
  }

  @Override
  public String description() {
    return "long(" + field + ')';
  }

  public long externalToLong(String extVal) {
    return Long.parseLong(extVal);
  }

  public Object longToObject(long val) {
    return val;
  }

  public String longToString(long val) {
    return longToObject(val).toString();
  }

  @Override
  public FuncValues getValues(Map context, AtomicReaderContext readerContext) throws IOException {
    final FieldCache.Longs arr = cache.getLongs(readerContext.reader(), field, parser, true);
    final Bits valid = cache.getDocsWithField(readerContext.reader(), field);

    return new LongDocValues(this) {
      @Override
      public long longVal(int doc) {
        return arr.get(doc);
      }

      @Override
      public boolean exists(int doc) {
        return arr.get(doc) != 0 || valid.get(doc);
      }

      @Override
      public Object objectVal(int doc) {
        return valid.get(doc) ? longToObject(arr.get(doc)) : null;
      }

      @Override
      public String strVal(int doc) {
        return valid.get(doc) ? longToString(arr.get(doc)) : null;
      }

      @Override
      protected long externalToLong(String extVal) {
        return LongFieldSource.this.externalToLong(extVal);
      }

      @Override
      public ValueFiller getValueFiller() {
        return new ValueFiller() {
          private final MutableValueLong mval = newMutableValueLong();

          @Override
          public MutableValue getValue() {
            return mval;
          }

          @Override
          public void fillValue(int doc) {
            mval.value = arr.get(doc);
            mval.exists = mval.value != 0 || valid.get(doc);
          }
        };
      }

    };
  }

  protected MutableValueLong newMutableValueLong() {
    return new MutableValueLong();
  }

  @Override
  public boolean equals(Object o) {
    if (o.getClass() != this.getClass()) return false;
    LongFieldSource other = (LongFieldSource) o;
    return super.equals(other)
        && (this.parser == null ? other.parser == null :
        this.parser.getClass() == other.parser.getClass());
  }

  @Override
  public int hashCode() {
    int h = parser == null ? this.getClass().hashCode() : parser.getClass().hashCode();
    h += super.hashCode();
    return h;
  }
}
