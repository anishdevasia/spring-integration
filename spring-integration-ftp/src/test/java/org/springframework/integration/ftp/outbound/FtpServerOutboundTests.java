/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.ftp.outbound;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.List;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.ftp.FtpServerRule;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class FtpServerOutboundTests {

	@ClassRule
	public static final FtpServerRule FTP_SERVER = new FtpServerRule(FtpServerOutboundTests.class.getSimpleName());

	@Autowired
	private PollableChannel output;

	@Autowired
	private DirectChannel inboundGet;

	@Autowired
	private DirectChannel invalidDirExpression;

	@Autowired
	private DirectChannel inboundMGet;

	@Before
	public void setup() {
		FtpServerRule.recursiveDelete(FTP_SERVER.getTargetLocalDirectory());
		FtpServerRule.recursiveDelete(FTP_SERVER.getTargetFtpDirectory());
	}

	@Test
	public void testInt2866LocalDirectoryExpressionGET() {
		String dir = "ftpSource/";
		this.inboundGet.send(new GenericMessage<Object>(dir + "ftpSource1.txt"));
		Message<?> result = this.output.receive(1000);
		assertNotNull(result);
		File localFile = (File) result.getPayload();
		assertThat(localFile.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
				Matchers.containsString(dir.toUpperCase()));

		dir = "ftpSource/subFtpSource/";
		this.inboundGet.send(new GenericMessage<Object>(dir + "subFtpSource1.txt"));
		result = this.output.receive(1000);
		assertNotNull(result);
		localFile = (File) result.getPayload();
		assertThat(localFile.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
				Matchers.containsString(dir.toUpperCase()));
	}

	@Test
	public void testInt2866InvalidLocalDirectoryExpression() {
		try {
			this.invalidDirExpression.send(new GenericMessage<Object>("/ftpSource/ftpSource1.txt"));
			fail("Exception expected.");
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			assertThat(cause, Matchers.instanceOf(IllegalArgumentException.class));
			assertThat(cause.getMessage(), Matchers.startsWith("Failed to make local directory"));
		}
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInt2866LocalDirectoryExpressionMGET() {
		String dir = "ftpSource/";
		this.inboundMGet.send(new GenericMessage<Object>(dir + "*.txt"));
		Message<?> result = this.output.receive(1000);
		assertNotNull(result);
		List<File> localFiles = (List<File>) result.getPayload();

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
					Matchers.containsString(dir));
		}

		dir = "ftpSource/subFtpSource/";
		this.inboundMGet.send(new GenericMessage<Object>(dir + "*.txt"));
		result = this.output.receive(1000);
		assertNotNull(result);
		localFiles = (List<File>) result.getPayload();

		for (File file : localFiles) {
			assertThat(file.getPath().replaceAll(java.util.regex.Matcher.quoteReplacement(File.separator), "/"),
					Matchers.containsString(dir));
		}
	}

	public static String localDirectory() {
		return FTP_SERVER.getTargetLocalDirectory().getAbsolutePath() + File.separator;
	}


}
