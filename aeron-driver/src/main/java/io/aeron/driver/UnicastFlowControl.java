/*
 * Copyright 2014-2023 Real Logic Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aeron.driver;

import io.aeron.driver.media.UdpChannel;
import io.aeron.protocol.SetupFlyweight;
import io.aeron.protocol.StatusMessageFlyweight;
import org.agrona.concurrent.status.CountersManager;

import java.net.InetSocketAddress;

import static io.aeron.logbuffer.LogBufferDescriptor.computePosition;

/**
 * Default unicast sender flow control strategy.
 * <p>
 * Max of right edges.
 * No tracking of receivers.
 */
public class UnicastFlowControl implements FlowControl
{
    /**
     * Singleton instance which can be used to avoid allocation.
     */
    public static final UnicastFlowControl INSTANCE = new UnicastFlowControl();

    /**
     * Multiple of receiver window to allow for a retransmit action.
     */
    private static final int RETRANSMIT_RECEIVER_WINDOW_MULTIPLE = 16;

    /**
     * {@inheritDoc}
     */
    public long onStatusMessage(
        final StatusMessageFlyweight flyweight,
        final InetSocketAddress receiverAddress,
        final long senderLimit,
        final int initialTermId,
        final int positionBitsToShift,
        final long timeNs)
    {
        final long position = computePosition(
            flyweight.consumptionTermId(),
            flyweight.consumptionTermOffset(),
            positionBitsToShift,
            initialTermId);

        return Math.max(senderLimit, position + flyweight.receiverWindowLength());
    }

    /**
     * {@inheritDoc}
     */
    public void onTriggerSendSetup(
        final StatusMessageFlyweight flyweight,
        final InetSocketAddress receiverAddress,
        final long timeNs)
    {
    }

    /**
     * {@inheritDoc}
     */
    public long onSetup(
        final SetupFlyweight flyweight,
        final long senderLimit,
        final long senderPosition,
        final int positionBitsToShift,
        final long timeNs)
    {
        return senderLimit;
    }

    /**
     * {@inheritDoc}
     */
    public void initialize(
        final MediaDriver.Context context,
        final CountersManager countersManager,
        final UdpChannel udpChannel,
        final int streamId,
        final int sessionId,
        final long registrationId,
        final int initialTermId,
        final int termBufferLength)
    {
    }

    /**
     * {@inheritDoc}
     */
    public void close()
    {
    }

    /**
     * {@inheritDoc}
     */
    public long onIdle(final long timeNs, final long senderLimit, final long senderPosition, final boolean isEos)
    {
        return senderLimit;
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasRequiredReceivers()
    {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int maxRetransmissionLength(
        final long resendPosition,
        final int resendLength,
        final int termBufferLength,
        final int mtuLength)
    {
        final int estimatedWindowLength = Configuration.receiverWindowLength(
            termBufferLength, Configuration.INITIAL_WINDOW_LENGTH_DEFAULT);

        return Math.min(RETRANSMIT_RECEIVER_WINDOW_MULTIPLE * estimatedWindowLength, resendLength);
    }
}
