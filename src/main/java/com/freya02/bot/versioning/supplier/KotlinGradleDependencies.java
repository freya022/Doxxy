package com.freya02.bot.versioning.supplier;

public class KotlinGradleDependencies implements BuildToolDependencies {
	private static final String BC_SCRIPT = """
			repositories {
				mavenCentral()
				maven("https://jitpack.io")
			}
			
			dependencies {
				implementation("%s:%s:%s")
				implementation("%s:%s:%s")
			}""";

	private static final String JDA4_SCRIPT = """
			repositories {
			    mavenCentral()
			    maven("https://m2.dv8tion.net/releases")
			}
			
			dependencies {
				implementation("%s:%s:%s")
			}
			""";

	private static final String JDA5_SCRIPT = """
			dependencies {
				implementation("%s:%s:%s")
			}""";

	private static final String JDA5_JITPACK_SCRIPT = """
			repositories {
				mavenCentral()
				maven("https://jitpack.io")
			}
			
			dependencies {
				implementation("%s:%s:%s")
			}""";

	@Override
	public String getBCDependencyFormatString() {
		return BC_SCRIPT;
	}

	@Override
	public String getJDA4DependencyFormatString() {
		return JDA4_SCRIPT;
	}

	@Override
	public String getJDA5DependencyFormatString() {
		return JDA5_SCRIPT;
	}

	@Override
	public String getJDA5JitpackDependencyFormatString() {
		return JDA5_JITPACK_SCRIPT;
	}
}
