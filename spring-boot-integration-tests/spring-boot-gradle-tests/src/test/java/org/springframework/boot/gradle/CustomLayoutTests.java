/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.gradle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.charset.Charset;
import java.util.jar.JarFile;

import org.gradle.tooling.ProjectConnection;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.util.StreamUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for custom layout
 *
 * @author Dave Syer
 */
public class CustomLayoutTests {

	private static final String BOOT_VERSION = Versions.getBootVersion();

	private static ProjectConnection project;

	@Rule
	public OutputCapture output = new OutputCapture();

	@BeforeClass
	public static void createProject() throws IOException {
		project = new ProjectCreator().createProject("custom-layout");
	}

	@Test
	public void customLayout() throws Exception {
		project.newBuild().forTasks("clean", "build")
				.withArguments("-PbootVersion=" + BOOT_VERSION)
				.setStandardOutput(System.out).run();
		File buildLibs = new File("target/custom-layout/build/libs");
		File executableJar = new File(buildLibs, "custom-layout.jar");
		assertThat(executableJar).exists();
		assertThat(isIncluded(executableJar, "lib/spring-boot-" + BOOT_VERSION + ".jar"))
				.isTrue();
		ProcessBuilder builder = new ProcessBuilder(javaCommand(),
				"-Dloader.config.location=classpath:classes/loader.properties", "-jar",
				executableJar.getAbsolutePath());
		builder.redirectErrorStream(true);
		File file = new File("target/custom-layout/build/out.log");
		builder.redirectOutput(Redirect.to(file));
		Process started = builder.start();
		started.waitFor();
		assertThat(StreamUtils.copyToString(new FileInputStream(file),
				Charset.forName("utf-8"))).contains("Started Application");
	}

	private boolean isIncluded(File repackageFile, String path) throws IOException {
		JarFile jarFile = new JarFile(repackageFile);
		try {
			return jarFile.getEntry(path) != null;
		}
		finally {
			jarFile.close();
		}
	}

	public static String javaCommand() {
		String javaHome = System.getProperty("java.home");
		if (javaHome == null) {
			return "java"; // Hope it's in PATH
		}
		File javaExecutable = new File(javaHome, "bin/java");
		if (javaExecutable.exists() && javaExecutable.canExecute()) {
			return javaExecutable.getAbsolutePath();
		}
		return "java";
	}
}
