/*
 * Copyright 2018 ConsenSys AG.
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
package tech.pegasys.pantheon.ethereum.mainnet.headervalidationrules;

import static org.apache.logging.log4j.LogManager.getLogger;

import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.mainnet.DetachedBlockHeaderValidationRule;

import java.util.function.Function;

import org.apache.logging.log4j.Logger;

public class ConstantFieldValidationRule<T> implements DetachedBlockHeaderValidationRule {
  private static final Logger LOG = getLogger();
  private final T expectedValue;
  private final Function<BlockHeader, T> accessor;
  private final String fieldName;

  public ConstantFieldValidationRule(
      final String fieldName, final Function<BlockHeader, T> accessor, final T expectedValue) {
    this.expectedValue = expectedValue;
    this.accessor = accessor;
    this.fieldName = fieldName;
  }

  @Override
  public boolean validate(final BlockHeader header, final BlockHeader parent) {
    final T actualValue = accessor.apply(header);
    if (!actualValue.equals(expectedValue)) {
      LOG.trace(
          "{} failed validation. Actual != Expected ({} != {}).",
          fieldName,
          actualValue,
          expectedValue);
      return false;
    }
    return true;
  }
}
