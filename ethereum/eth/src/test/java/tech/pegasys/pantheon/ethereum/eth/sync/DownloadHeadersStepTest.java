/*
 * Copyright 2019 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.ethereum.eth.sync;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.chain.MutableBlockchain;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.eth.manager.EthProtocolManager;
import tech.pegasys.pantheon.ethereum.eth.manager.EthProtocolManagerTestUtil;
import tech.pegasys.pantheon.ethereum.eth.manager.RespondingEthPeer;
import tech.pegasys.pantheon.ethereum.eth.manager.ethtaskutils.BlockchainSetupUtil;
import tech.pegasys.pantheon.ethereum.mainnet.HeaderValidationMode;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class DownloadHeadersStepTest {

  private static ProtocolSchedule<Void> protocolSchedule;
  private static ProtocolContext<Void> protocolContext;
  private static MutableBlockchain blockchain;

  private EthProtocolManager ethProtocolManager;
  private DownloadHeadersStep<Void> downloader;
  private CheckpointRange checkpointRange;

  @BeforeClass
  public static void setUpClass() {
    final BlockchainSetupUtil<Void> setupUtil = BlockchainSetupUtil.forTesting();
    setupUtil.importFirstBlocks(20);
    protocolSchedule = setupUtil.getProtocolSchedule();
    protocolContext = setupUtil.getProtocolContext();
    blockchain = protocolContext.getBlockchain();
  }

  @Before
  public void setUp() {
    ethProtocolManager =
        EthProtocolManagerTestUtil.create(blockchain, protocolContext.getWorldStateArchive());
    downloader =
        new DownloadHeadersStep<>(
            protocolSchedule,
            protocolContext,
            ethProtocolManager.ethContext(),
            () -> HeaderValidationMode.DETACHED_ONLY,
            new NoOpMetricsSystem());

    checkpointRange =
        new CheckpointRange(
            blockchain.getBlockHeader(1).get(), blockchain.getBlockHeader(10).get());
  }

  @Test
  public void shouldRetrieveHeadersForCheckpointRange() {
    final RespondingEthPeer peer = EthProtocolManagerTestUtil.createPeer(ethProtocolManager, 1000);
    final CompletableFuture<CheckpointRangeHeaders> result = downloader.apply(checkpointRange);

    peer.respond(RespondingEthPeer.blockchainResponder(blockchain));

    // The start of the range should have been imported as part of the previous batch hence 2-10.
    assertThat(result)
        .isCompletedWithValue(new CheckpointRangeHeaders(checkpointRange, headersFromChain(2, 10)));
  }

  @Test
  public void shouldCancelRequestToPeerWhenReturnedFutureIsCancelled() {
    final RespondingEthPeer peer = EthProtocolManagerTestUtil.createPeer(ethProtocolManager, 1000);

    final CompletableFuture<CheckpointRangeHeaders> result = this.downloader.apply(checkpointRange);

    result.cancel(true);

    EthProtocolManagerTestUtil.disableEthSchedulerAutoRun(ethProtocolManager);

    peer.respond(RespondingEthPeer.blockchainResponder(blockchain));

    assertThat(EthProtocolManagerTestUtil.getPendingFuturesCount(ethProtocolManager)).isZero();
  }

  private List<BlockHeader> headersFromChain(final long startNumber, final long endNumber) {
    final List<BlockHeader> headers = new ArrayList<>();
    for (long i = startNumber; i <= endNumber; i++) {
      headers.add(blockchain.getBlockHeader(i).get());
    }
    return Collections.unmodifiableList(headers);
  }
}
