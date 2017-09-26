/**
 * 
 */
package org.cboard.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author wangFeng
 *
 */
public abstract class SystemUtil {

	private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");
	private static ThreadLocalRandom random = ThreadLocalRandom.current();

	private static final int START = 100;
	private static final int END = 999;

	private SystemUtil() {
	}

	public static String getRandomNumber() {
		StringBuilder result = new StringBuilder("");
		result.append(LocalDateTime.now().format(FORMAT)).append(random.nextInt(START, END));
		return result.toString();
	}

	public static String getOsName() {
		return System.getProperties().getProperty("os.name");
	}
}
