package com.oanda.v20.v20sample;

import com.oanda.v20.account.AccountID;
import com.oanda.v20.primitives.InstrumentName;

/**
 * This is the configuration object used by the various examples to connect to
 * one of the OANDA trading systems.  Please fill them in with sane values.
 *
 * @param      URL         The fxTrade or fxPractice API URL
 * @param      TOKEN       The OANDA API Personal Access token
 * @param      ACCOUNTID   A valid v20 trading account ID that {@code TOKEN} has
 *                         permissions to take action on
 * @param      INSTRUMENT  A valid tradeable instrument for the given {@code
 *                         ACCOUNTID}
 */
public class Config {
    private Config() {}
    public static final String URL = "<< URL >>";
    public static final String TOKEN = "<< TOKEN >>";
    public static final AccountID ACCOUNTID = new AccountID("<< ACCOUNTID >>");
    public static final InstrumentName INSTRUMENT  = new InstrumentName("<< INSTRUMENT >>");
}
