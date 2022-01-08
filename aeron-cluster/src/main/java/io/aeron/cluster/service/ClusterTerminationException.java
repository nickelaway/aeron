/*
 * Copyright 2014-2022 Real Logic Limited.
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
package io.aeron.cluster.service;

import org.agrona.concurrent.AgentTerminationException;

/**
 * Used to terminate the {@link org.agrona.concurrent.Agent} within a cluster in an expected fashion.
 */
public class ClusterTerminationException extends AgentTerminationException
{
    private static final long serialVersionUID = -2705156056823180407L;
}
