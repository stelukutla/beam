/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.beam.runners.flink.translation.functions;

import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.values.KV;

import org.apache.flink.api.common.functions.GroupCombineFunction;
import org.apache.flink.util.Collector;

import java.util.Iterator;

/**
 * Flink {@link org.apache.flink.api.common.functions.GroupCombineFunction} for executing a
 * {@link org.apache.beam.sdk.transforms.Combine.PerKey} operation. This reads the input
 * {@link org.apache.beam.sdk.values.KV} elements VI, extracts the key and emits accumulated
 * values which have the intermediate format VA.
 */
public class FlinkPartialReduceFunction<K, VI, VA> implements GroupCombineFunction<KV<K, VI>, KV<K, VA>> {

  private final Combine.KeyedCombineFn<K, VI, VA, ?> keyedCombineFn;

  public FlinkPartialReduceFunction(Combine.KeyedCombineFn<K, VI, VA, ?>
                                        keyedCombineFn) {
    this.keyedCombineFn = keyedCombineFn;
  }

  @Override
  public void combine(Iterable<KV<K, VI>> elements, Collector<KV<K, VA>> out) throws Exception {

    final Iterator<KV<K, VI>> iterator = elements.iterator();
    // create accumulator using the first elements key
    KV<K, VI> first = iterator.next();
    K key = first.getKey();
    VI value = first.getValue();
    VA accumulator = keyedCombineFn.createAccumulator(key);
    accumulator = keyedCombineFn.addInput(key, accumulator, value);

    while(iterator.hasNext()) {
      value = iterator.next().getValue();
      accumulator = keyedCombineFn.addInput(key, accumulator, value);
    }

    out.collect(KV.of(key, accumulator));
  }
}
