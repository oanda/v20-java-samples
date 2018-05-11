package com.oanda.v20.v20sample;

import java.util.List;

import com.oanda.v20.Context;
import com.oanda.v20.ContextBuilder;
import com.oanda.v20.account.Account;
import com.oanda.v20.account.AccountGetResponse;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.account.AccountListResponse;
import com.oanda.v20.account.AccountProperties;
import com.oanda.v20.order.MarketOrderRequest;
import com.oanda.v20.order.OrderCreateRequest;
import com.oanda.v20.order.OrderCreateResponse;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.trade.TradeCloseRequest;
import com.oanda.v20.trade.TradeCloseResponse;
import com.oanda.v20.trade.TradeSpecifier;
import com.oanda.v20.transaction.OrderFillTransaction;
import com.oanda.v20.transaction.TradeReduce;
import com.oanda.v20.transaction.TransactionID;

/**
 * This is a brief example that explicitly shows each step in the process of preparing,
 * executing, and processing results from a few different queries.  This style is only
 * for demonstration purposes and is therefore overly verbose.
 * <p>
 * For the idiomatic style, see {@link TestTradesAndOrders}
 * <p>
 * @author gentili
 *
 */
public abstract class StepByStepOrder {

    public static void main(String[] args) {
        Context ctx = new ContextBuilder(Config.URL)
        		.setToken(Config.TOKEN)
        		.setApplication("StepByStepOrder")
        		.build();
        
        AccountID accountId = Config.ACCOUNTID;
        InstrumentName tradeableInstrument = Config.INSTRUMENT;

        // EXAMPLE: No parameters
        System.out.println("Make sure we have a valid account");
        try {
            // Execute the request and obtain a response object
            AccountListResponse response = ctx.account.list();
            // Retrieve account list from response object
            List<AccountProperties> accountProperties;
            accountProperties = response.getAccounts();
            // Check for the configured account
            boolean hasaccount = false;
            for (AccountProperties account : accountProperties) {
                if (account.getId().equals(accountId))
                    hasaccount = true;
            }
            if (!hasaccount)
                throw new TestFailureException("Account "+accountId+" not found");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // EXAMPLE: URL path parameter
        System.out.println("Make sure the account has a non zero balance");
        try {
            // Execute the request and retrieve a response object
            AccountGetResponse response = ctx.account.get(accountId);
            // Retrieve the contents of the result
            Account account;
            account = response.getAccount();
            // Check the balance
            if (account.getBalance().doubleValue() <= 0.0)
                throw new TestFailureException("Account "+accountId+" balance "+account.getBalance()+" <= 0");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // EXAMPLE: Complex body parameter
        System.out.println("Place a Market Order");
        TransactionID tradeId;
        try {
            // Create the new request
            OrderCreateRequest request = new OrderCreateRequest(accountId);
            // Create the required body parameter
            MarketOrderRequest marketorderrequest = new MarketOrderRequest();
            // Populate the body parameter fields
            marketorderrequest.setInstrument(tradeableInstrument);
            marketorderrequest.setUnits(10);
            // Attach the body parameter to the request
            request.setOrder(marketorderrequest);
            // Execute the request and obtain the response object
            OrderCreateResponse response = ctx.order.create(request);
            // Extract the Order Fill transaction for the executed Market Order
            OrderFillTransaction transaction = response.getOrderFillTransaction();
            // Extract the trade ID of the created trade from the transaction and keep it for future action
            tradeId = transaction.getId();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // EXAMPLE: Take action on existing entity
        System.out.println("Close a Trade");
        try {
            // Execute the request and retrieve the response object
            TradeCloseResponse response = ctx.trade.close(
            		new TradeCloseRequest(accountId, new TradeSpecifier(tradeId.toString())));
            // Extract the order fill transaction describing the trade close action
            OrderFillTransaction transaction = response.getOrderFillTransaction();
            // Extract the list of trades that were closed by the request
            List<TradeReduce> trades = transaction.getTradesClosed();
            // Check if single trade closed
            if (trades.size() != 1)
                throw new TestFailureException("Only 1 trade was expected to be closed");
            // Extract the single closed trade
            TradeReduce trade = trades.get(0);
            // Check if trade closed was the one we asked to be closed
            if (!trade.getTradeID().equals(tradeId))
                throw new TestFailureException("The wrong trade was closed");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        System.out.println("Done");
    }
}
