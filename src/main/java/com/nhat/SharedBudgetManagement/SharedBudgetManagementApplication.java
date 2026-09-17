package com.nhat.SharedBudgetManagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@SpringBootApplication
public class SharedBudgetManagementApplication {

	public static void main(String[] args) {
		loadDotenv();
		SpringApplication.run(SharedBudgetManagementApplication.class, args);
	}

	/**
	 * Tự động nạp các biến từ file .env vào System Properties khi chạy Local,
	 * nếu biến đó chưa được định nghĩa trong System Environment của OS/Docker.
	 */
	private static void loadDotenv() {
		File envFile = new File(".env");
		if (envFile.exists() && envFile.isFile()) {
			try {
				Files.lines(envFile.toPath())
						.map(String::trim)
						.filter(line -> !line.isEmpty() && !line.startsWith("#") && line.contains("="))
						.forEach(line -> {
							int idx = line.indexOf('=');
							String key = line.substring(0, idx).trim();
							String val = line.substring(idx + 1).trim();
							if ((val.startsWith("\"") && val.endsWith("\""))
									|| (val.startsWith("'") && val.endsWith("'"))) {
								val = val.substring(1, val.length() - 1);
							}
							if (System.getProperty(key) == null && System.getenv(key) == null) {
								System.setProperty(key, val);
							}
						});
			} catch (IOException ignored) {
			}
		}
	}

}
