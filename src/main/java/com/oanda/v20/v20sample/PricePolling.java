package com.oanda.v20.v20sample;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.oanda.v20.Context;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.pricing.Price;
import com.oanda.v20.pricing.PricingGetRequest;
import com.oanda.v20.pricing.PricingGetResponse;
import com.oanda.v20.primitives.DateTime;

public class PricePolling {

    public static void main(String[] args) {
        Context ctx = new Context(Config.URL, Config.TOKEN);
        AccountID accountId = Config.ACCOUNTID;
        List<String> instruments = new ArrayList<>(
                Arrays.asList("EUR_USD", "USD_JPY", "GBP_USD", "USD_CHF"));

        // Poll for prices
        try {
            PricingGetRequest request = new PricingGetRequest(accountId, instruments);

            DateTime since = null;

            while (true) {
                if (since != null)
                {
                    System.out.println("Polling since " + since);
                    request.setSince(since);
                }
                PricingGetResponse resp = ctx.pricing.get(request);

                for (Price price : resp.getPrices())
                    System.out.println(price);
                since = resp.getTime();

                Thread.sleep(1000);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
