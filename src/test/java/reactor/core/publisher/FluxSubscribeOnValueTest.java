/*
 * Copyright (c) 2011-2016 Pivotal Software Inc, All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package reactor.core.publisher;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Test;
import reactor.core.Exceptions;
import reactor.core.Fuseable;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;

import static org.junit.Assert.assertTrue;

public class FluxSubscribeOnValueTest {

	ConcurrentMap<Integer, Integer> execs = new ConcurrentHashMap<>();

	@Test
	public void testSubscribeOnValueFusion() {

		StepVerifier.create(Flux.range(1, 100)
		                        .flatMap(f -> Flux.just(f)
		                                          .subscribeOn(Schedulers.parallel())
		                                          .log()
		                                          .map(this::slow)))
		            .expectFusion(Fuseable.ASYNC, Fuseable.NONE)
		            .expectNextCount(100)
		            .verifyComplete();

		int minExec = 2;

		for (Integer counted : execs.values()) {
			assertTrue("Thread used less than " + minExec + " " + "times",
					counted >= minExec);
		}

	}

	int slow(int slow){
		try {
			execs.computeIfAbsent(Thread.currentThread()
			                            .hashCode(), i -> 0);
			execs.compute(Thread.currentThread()
			                    .hashCode(), (k, v) -> v + 1);
			Thread.sleep(10);
			return slow;
		}
		catch (InterruptedException e) {
			throw Exceptions.bubble(e);
		}
	}
}