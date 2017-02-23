package com.oanda.v20.v20sample;

import com.oanda.v20.Account;
import com.oanda.v20.AccountProperties;
import com.oanda.v20.Context;
import com.oanda.v20.Context.AccountContext.GetRequest;
import com.oanda.v20.Context.AccountContext.GetResponse;
import com.oanda.v20.Context.AccountContext.ListRequest;
import com.oanda.v20.Context.AccountContext.ListResponse;
import com.oanda.v20.Context.OrderContext.CreateRequest;
import com.oanda.v20.Context.OrderContext.CreateResponse;
import com.oanda.v20.Context.TradeContext.CloseRequest;
import com.oanda.v20.Context.TradeContext.CloseResponse;
import com.oanda.v20.MarketOrderRequest;
import com.oanda.v20.OrderFillTransaction;
import com.oanda.v20.TradeReduce;

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
		Context ctx = new Context(Config.url, Config.token);
        String accountId = Config.accountId;
        String tradeableInstrument = Config.instrument;

        // EXAMPLE: No parameters
        System.out.println("Make sure we have a valid account");
		try {
			// Create the request object
			ListRequest request = ctx.account.list();
			// Execute the request and obtain a response object
			ListResponse response = request.execute();
			// Retrieve account list from response object
	        AccountProperties[] accountProperties;
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
        	// Create the request object
			GetRequest request = ctx.account.get(accountId);
			// Execute the request and retrieve a response object
			GetResponse response = request.execute();
			// Retrieve the contents of the result
	        Account account;
			account = response.getAccount();
			// Check the balance
			if (account.getBalance() <= 0.0)
				throw new TestFailureException("Account "+accountId+" balance "+account.getBalance()+" <= 0");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
        // EXAMPLE: Complex body parameter
        System.out.println("Place a Market Order");
        String tradeId;
        try {
        	// Create the new request
        	CreateRequest request = ctx.order.create(accountId);
        	// Create the required body parameter
			MarketOrderRequest marketorderrequest = new MarketOrderRequest();
			// Populate the body parameter fields
			marketorderrequest.setInstrument(tradeableInstrument);
			marketorderrequest.setUnits(10);
			// Attach the body parameter to the request
			request.order(marketorderrequest);
			// Execute the request and obtain the response object
			CreateResponse response = request.execute();
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
        	// Create the Close Trade Request
        	CloseRequest request = ctx.trade.close(accountId, tradeId);
        	// Execute the request and retrieve the response object
			CloseResponse response = request.execute();
			// Extract the order fill transaction describing the trade close action
			OrderFillTransaction transaction = response.getOrderFillTransaction();
			// Extract the list of trades that were closed by the request 
			TradeReduce[] trades = transaction.getTradesClosed();
			// Check if single trade closed 
			if (trades.length != 1)
				throw new TestFailureException("Only 1 trade was expected to be closed");
			// Extract the single closed trade
			TradeReduce trade = trades[0];
			// Check if trade closed was the one we asked to be closed
			if (!trade.getTradeID().equals(tradeId))
				throw new TestFailureException("The wrong trade was closed");
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
        System.out.println("Done");
	}
}
