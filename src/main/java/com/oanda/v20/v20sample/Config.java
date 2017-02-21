package com.oanda.v20.v20sample;

/**
 * This is the configuration object used by the various examples to connect to
 * one of the OANDA trading systems.  Please fill them in with sane values.
 * @param url The fxTrade or fxPractice API url
 * @param token The OANDA API Personal Access token
 * @param accountId A valid v20 trading account ID that {@code token} has permissions to take action on
 * @param instrument A valid tradeable instrument for the given {@code accountId}
 * @author gentili
 *
 */
public class Config {
	private Config() {} // Singleton
	public static String url = "";
	public static String token = "";
	public static String accountId = "";
	public static String instrument  = "";
}
