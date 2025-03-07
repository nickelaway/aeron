/*
 * Copyright 2014-2025 Real Logic Limited.
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
package io.aeron.samples.raw;

import io.aeron.driver.Configuration;
import org.HdrHistogram.Histogram;
import org.agrona.BitUtil;
import org.agrona.SystemUtil;
import org.agrona.concurrent.HighResolutionTimer;
import org.agrona.concurrent.SigInt;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.LockSupport;

import static io.aeron.samples.raw.Common.init;

/**
 * Benchmark used to calculate latency of underlying system.
 *
 * @see ReceiveSendUdpPong
 */
public class SendReceiveUdpPing
{
    /**
     * Main method for launching the process.
     *
     * @param args passed to the process.
     * @throws IOException if an error occurs with the channel.
     */
    public static void main(final String[] args) throws IOException
    {
        int numChannels = 1;
        if (1 <= args.length)
        {
            numChannels = Integer.parseInt(args[0]);
        }

        String remoteHost = "localhost";
        if (2 <= args.length)
        {
            remoteHost = args[1];
        }

        if (SystemUtil.isWindows())
        {
            HighResolutionTimer.enable();
        }

        System.out.printf("Number of channels: %d, Remote host: %s%n", numChannels, remoteHost);

        final Histogram histogram = new Histogram(TimeUnit.SECONDS.toNanos(10), 3);
        final ByteBuffer buffer = ByteBuffer.allocateDirect(Configuration.MTU_LENGTH_DEFAULT);

        final DatagramChannel[] receiveChannels = new DatagramChannel[numChannels];
        for (int i = 0; i < receiveChannels.length; i++)
        {
            receiveChannels[i] = DatagramChannel.open();
            init(receiveChannels[i]);
            receiveChannels[i].bind(new InetSocketAddress("0.0.0.0", Common.PONG_PORT + i));
        }

        final InetSocketAddress sendAddress = new InetSocketAddress(remoteHost, Common.PING_PORT);
        final DatagramChannel sendChannel = DatagramChannel.open();
        init(sendChannel);

        final AtomicBoolean running = new AtomicBoolean(true);
        SigInt.register(() -> running.set(false));

        while (running.get())
        {
            measureRoundTrip(histogram, sendAddress, buffer, receiveChannels, sendChannel, running);

            histogram.reset();
            System.gc();
            LockSupport.parkNanos(1_000_000_000L);
        }
    }

    private static void measureRoundTrip(
        final Histogram histogram,
        final InetSocketAddress sendAddress,
        final ByteBuffer buffer,
        final DatagramChannel[] receiveChannels,
        final DatagramChannel sendChannel,
        final AtomicBoolean running)
        throws IOException
    {
        for (int sequenceNumber = 0; sequenceNumber < Common.NUM_MESSAGES; sequenceNumber++)
        {
            final long timestampNs = System.nanoTime();

            buffer.clear();
            buffer.putLong(sequenceNumber);
            buffer.putLong(timestampNs);
            buffer.flip();

            sendChannel.send(buffer, sendAddress);

            buffer.clear();
            boolean available = false;
            while (!available)
            {
                Thread.onSpinWait();
                if (!running.get())
                {
                    return;
                }

                for (int i = receiveChannels.length - 1; i >= 0; i--)
                {
                    if (null != receiveChannels[i].receive(buffer))
                    {
                        available = true;
                        break;
                    }
                }
            }

            final long receivedSequenceNumber = buffer.getLong(0);
            final long receivedTimestamp = buffer.getLong(BitUtil.SIZE_OF_LONG);

            if (receivedSequenceNumber != sequenceNumber)
            {
                throw new IllegalStateException("Data Loss: " + sequenceNumber + " to " + receivedSequenceNumber);
            }

            final long durationNs = System.nanoTime() - receivedTimestamp;
            histogram.recordValue(durationNs);
        }

        histogram.outputPercentileDistribution(System.out, 1000.0);
    }
}
