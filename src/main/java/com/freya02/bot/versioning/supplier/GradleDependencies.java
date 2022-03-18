package com.freya02.bot.versioning.supplier;

public class GradleDependencies implements BuildToolDependencies {
	private static final String BC_XML = """
			repositories {
				mavenCentral()
				maven { url 'https://jitpack.io' }
			}
			
			dependencies {
				implementation '%s:%s:%s'
				implementation '%s:%s:%s'
			}""";

	private static final String JDA4_XML = """
			repositories {
			    mavenCentral()
			    maven {
			        name 'm2-dv8tion'
			        url 'https://m2.dv8tion.net/releases'
			    }
			}
			         
			dependencies {
				implementation '%s:%s:%s'
			}
			""";

	private static final String JDA5_XML = """
			dependencies {
				implementation '%s:%s:%s'
			}""";

	private static final String JDA5_JITPACK_XML = """
			repositories {
				mavenCentral()
				maven { url 'https://jitpack.io' }
			}
			
			dependencies {
				implementation '%s:%s:%s'
			}""";

	@Override
	public String getBCDependencyFormatString() {
		return BC_XML;
	}

	@Override
	public String getJDA4DependencyFormatString() {
		return JDA4_XML;
	}

	@Override
	public String getJDA5DependencyFormatString() {
		return JDA5_XML;
	}

	@Override
	public String getJDA5JitpackDependencyFormatString() {
		return JDA5_JITPACK_XML;
	}
}
