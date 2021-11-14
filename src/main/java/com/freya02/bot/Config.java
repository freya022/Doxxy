package com.freya02.bot;

import com.google.gson.Gson;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("unused")
public class Config {
	private static Config instance;

	private String token;
	private DBConfig dbConfig;

	private Config() {}

	public static Config getConfig() {
		if (instance == null) {
			try {
				instance = new Gson().fromJson(Files.readString(Path.of("Config.json")), Config.class);
			} catch (IOException e) {
				throw new RuntimeException("Unable to load configs", e);
			}
		}

		return instance;
	}

	public String getToken() {
		return token;
	}

	public DBConfig getDbConfig() {
		return dbConfig;
	}

	public static class DBConfig {
		private String serverName;
		private int portNumber;
		private String user, password, dbName;

		public String getServerName() {
			return serverName;
		}

		public int getPortNumber() {
			return portNumber;
		}

		public String getUser() {
			return user;
		}

		public String getPassword() {
			return password;
		}

		public String getDbName() {
			return dbName;
		}
	}
}