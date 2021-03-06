package org.apache.solr.search;

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

import org.apache.lucene.index.AtomicReader;
import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.BinaryDocValues;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.FieldInfos;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexReaderContext;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.index.StoredFieldVisitor;
import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.Filter;
import org.apache.lucene.util.Bits;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.lucene.util.OpenBitSet;
import org.apache.lucene.util.OpenBitSetIterator;
import org.apache.solr.HSTest;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.AfterClass;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 *
 */
public class TestDocSetNative extends TestDocSet {

  {
    // flags to help only testing certain parts to help narrow down bugs
    intersect = true;
    union = true;
    andNot = true;
    intersectSz = true;
    unionSz = true;
    andNotSz = true;
  }

  @Override
  public void setUp() throws Exception {
    super.setUp();
    HSTest.startTracking();
  }

  @Override
  public void tearDown() throws Exception {
    HSTest.endTracking();
    super.tearDown();
  }


  public void testTracking() throws Exception {
    HSTest.startTracking();
    DocSet s1 = new SortedIntDocSetNative(new int[1]);
    boolean caught = false;
    try {
      DocSetBaseNative.debug(false);  // to test display

      HSTest.endTracking();
    } catch (Throwable th) {
      caught = true;
      SolrTestCaseJ4.log.info("SUCCESSFULLY CAUGHT: " + th);
    }
    assertTrue(caught);
    s1.decref();
  }

  public void testDoubleFree() throws Exception {
    DocSet s1 = new SortedIntDocSetNative(new int[1]);
    boolean caught = false;
    try {
      s1.decref();
      s1.decref();  // if we are really unlucky, this test could fail because the memory could be used for something else between the two decrefs...
    } catch (Throwable th) {
      caught = true;
      SolrTestCaseJ4.log.info("SUCCESSFULLY CAUGHT: " + th);
    }
    assertTrue(caught);
  }

  public void testTryWith() throws Exception {
    OpenBitSet obs = getRandomSet(10,5);

    try(
        DocSet a = getBitDocSetNative(obs);
        DocSet b = getIntDocSetNative(obs);
        DocSet c = a.intersection(b);
        DocSet d = a.union(b);
        ) {

      assertEquals(c.size(), a.intersectionSize(b));
      assertEquals(d.size(), a.unionSize(b));

    }

    // ref counting code should take care of testing this...
  }

  public void testSimple() throws Exception {
    OpenBitSet obs1 = getRandomSet(10,5);
    OpenBitSet obs2 = getRandomSet(10,5);

//    DocSet s1 = super.getHashDocSet(obs1);
//    DocSet s2 = super.getIntDocSetNative(obs2);
    DocSet s1 = super.getIntDocSetNative(obs1);
    DocSet s2 = super.getHashDocSet(obs2);

    doSingle(obs1, obs2, s1, s2);

    DocSet r1 = s1.intersection(s2);
    DocSet r2 = s2.intersection(s1);
    int c1 = s1.intersectionSize(s2);
    int c2 = s2.intersectionSize(s1);

    assertEquals(c1,c2);
    assertEquals(c1, r1.size());
    assertEquals(c1, r2.size());

    r1.decref();
    r2.decref();
    s1.decref();
    s2.decref();
  }

  /**
  @Override
  public DocSet getHashDocSet(OpenBitSet obs) {
    // return super.getHashDocSet(obs);
    // return super.getIntDocSetNative(obs);
    return super.getBitDocSetNative(obs);
  }

  @Override
  public DocSet getDocSlice(OpenBitSet obs) {
//    return super.getIntDocSetNative(obs);
    return super.getBitDocSetNative(obs);
  }
   **/

  @Override
  public DocSet getSmallSet(OpenBitSet obs) {
     return super.getIntDocSetNative(obs);
//     return super.getIntDocSetNative(obs);
//    return super.getBitDocSetNative(obs);
  }


  @Override
  public DocSet getBigSet(OpenBitSet obs) {
    // return super.getIntDocSetNative(obs);
    return super.getBitDocSetNative(obs);
  }


}
