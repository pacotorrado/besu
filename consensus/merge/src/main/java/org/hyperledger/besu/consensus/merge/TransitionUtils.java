/*
 * Copyright Hyperledger Besu Contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package org.hyperledger.besu.consensus.merge;

import org.hyperledger.besu.ethereum.ProtocolContext;
import org.hyperledger.besu.ethereum.core.BlockHeader;
import org.hyperledger.besu.ethereum.core.Difficulty;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class TransitionUtils<SwitchingObject> {
  private static final Logger LOG = LoggerFactory.getLogger(TransitionUtils.class);

  private final MergeContext mergeContext = PostMergeContext.get();
  private final SwitchingObject preMergeObject;
  private final SwitchingObject postMergeObject;

  public TransitionUtils(
      final SwitchingObject preMergeObject, final SwitchingObject postMergeObject) {
    this.preMergeObject = preMergeObject;
    this.postMergeObject = postMergeObject;
  }

  protected void dispatchConsumerAccordingToMergeState(final Consumer<SwitchingObject> consumer) {
    consumer.accept(mergeContext.isPostMerge() ? postMergeObject : preMergeObject);
  }

  protected <T> T dispatchFunctionAccordingToMergeState(
      final Function<SwitchingObject, T> function) {
    return function.apply(mergeContext.isPostMerge() ? postMergeObject : preMergeObject);
  }

  public SwitchingObject getPreMergeObject() {
    return preMergeObject;
  }

  SwitchingObject getPostMergeObject() {
    return postMergeObject;
  }

  public static boolean isTerminalProofOfWorkBlock(
      final BlockHeader header, final ProtocolContext context) {
    Optional<Difficulty> currentChainTotalDifficulty =
        context.getBlockchain().getTotalDifficultyByHash(header.getParentHash());
    Difficulty configuredTotalTerminalDifficulty =
        context.getConsensusContext(MergeContext.class).getTerminalTotalDifficulty();

    if (currentChainTotalDifficulty.isEmpty()) {
      LOG.warn("unable to get total difficulty, parent {} not found", header.getParentHash());
      return false;
    }
    if (currentChainTotalDifficulty
            .get()
            .add(header.getDifficulty() == null ? Difficulty.ZERO : header.getDifficulty())
            .greaterOrEqualThan(
                configuredTotalTerminalDifficulty) // adding would equal or go over limit
        && currentChainTotalDifficulty
            .get()
            .lessThan(configuredTotalTerminalDifficulty) // parent was under
    ) {
      return true;
    } else {
      return false;
    }
  }
}
