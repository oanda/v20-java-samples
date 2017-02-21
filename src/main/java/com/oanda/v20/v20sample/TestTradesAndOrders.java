package com.oanda.v20.v20sample;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;

import com.oanda.v20.Account;
import com.oanda.v20.AccountProperties;
import com.oanda.v20.ClientExtensions;
import com.oanda.v20.Context;
import com.oanda.v20.Context.AccountContext.ConfigureRequestException400;
import com.oanda.v20.Context.AccountContext.ConfigureResponse;
import com.oanda.v20.Context.OrderContext.CancelRequestException404;
import com.oanda.v20.Context.OrderContext.CreateRequestException400;
import com.oanda.v20.Context.OrderContext.CreateRequestException404;
import com.oanda.v20.Context.OrderContext.CreateResponse;
import com.oanda.v20.Context.OrderContext.ReplaceRequestException400;
import com.oanda.v20.Context.OrderContext.ReplaceRequestException404;
import com.oanda.v20.Context.OrderContext.ReplaceResponse;
import com.oanda.v20.Context.RequestException;
import com.oanda.v20.Context.TradeContext.CloseRequestException400;
import com.oanda.v20.Context.TradeContext.CloseRequestException404;
import com.oanda.v20.Context.TradeContext.SetclientextensionsRequestException400;
import com.oanda.v20.Context.TradeContext.SetclientextensionsRequestException404;
import com.oanda.v20.Context.TradeContext.SetdependentordersRequestException400;
import com.oanda.v20.Instrument;
import com.oanda.v20.LimitOrderRequest;
import com.oanda.v20.MarketOrderRequest;
import com.oanda.v20.Order;
import com.oanda.v20.OrderFillTransaction;
import com.oanda.v20.OrderState;
import com.oanda.v20.OrderType;
import com.oanda.v20.Position;
import com.oanda.v20.StopOrderRequest;
import com.oanda.v20.TakeProfitDetails;
import com.oanda.v20.TakeProfitOrderTransaction;
import com.oanda.v20.Trade;
import com.oanda.v20.TradeClientExtensionsModifyTransaction;
import com.oanda.v20.TradeReduce;
import com.oanda.v20.TradeState;
import com.oanda.v20.Transaction;
import com.oanda.v20.TransactionType;

/**
 * This is a small sample application that demonstrates idiomatic usage of the v20 java library,
 * including chained method calls and exception handling.
 * <p>
 * For a step by step breakdown of the actions of individual requests see {@link StepByStepOrder}
 * <p>
 * @author Michael Gentili
 */
public class TestTradesAndOrders {
	public static void main(String[] args) {
		//
		// Test a connection
		//
		
		Context ctx = new Context(Config.url, Config.token);
        String accountId = Config.accountId;
        String tradeableInstrument = Config.instrument;

        System.out.println("TEST - GET /accounts");
        System.out.println("CHECK 200 - The list of authorized Accounts has been provided, expecting "+accountId+" in list.");
		try {
	        AccountProperties[] accountProperties;
			accountProperties = ctx.account.list()
					.execute()
					.get_accounts();
			boolean hasaccount = false;
	        for (AccountProperties account : accountProperties) {
	        	if (account.get_id().equals(accountId))
	        		hasaccount = true;
	        }
	        if (!hasaccount)
	        	throw new RuntimeException("Account "+accountId+" not found");
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
		
		System.out.println("TEST - GET /accounts/{accountID}");
        System.out.println("CHECK 200 - The full Account details are provided, expecting balance > 0.");
        String firstTransId;  // Store the last transaction that happened before this session 
        try {
	        Account account;
			account = ctx.account.get(accountId).execute().get_account();
			firstTransId = account.get_lastTransactionID();
			if (account.get_balance() <= 0.0)
				throw new TestFailureException("Account "+accountId+" balance "+account.get_balance()+" <= 0");
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
 
        // get /accounts/{accountID}/summary
        // ctx.account.summary();
        // 200 - The Account summary  are provided

        System.out.println ("TEST - GET /accounts/{accountID}/instruments");
        System.out.println ("CHECK 200 - The list of tradeable instruments for the Account has been provided, expecting "+tradeableInstrument+" is tradeable.");
        try {
			Instrument[] instruments = ctx.account.instruments(accountId).execute().get_instruments();
			boolean istradeable = false;
			for (Instrument instrument : instruments) {
				if (instrument.get_name().equals(tradeableInstrument))
					istradeable = true;
			}
			if (!istradeable)
				throw new TestFailureException("Instrument "+tradeableInstrument+" is not tradeable");
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}

        System.out.println("TEST - PATCH /accounts/{accountID}/configuration");
        System.out.println("CHECK 400 - The Account could not be configured successfully, expecting [must specify parameter] exception.");
		try {
			ConfigureResponse result = ctx.account.configure(accountId).execute();
			throw new TestFailureException("Unexpected Success:" + result.get_clientConfigureTransaction());
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (ConfigureRequestException400 e) {
			// PASS
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
        // 200 - The Account was configured successfully.

        // get /accounts/{accountID}/changes
        // ctx.account.changes();
        // 200 - The Account state and changes are provided.

        // get /users/{userSpecifier}
        // ctx.user.getInfo();
        // 200 - The user information has been provided

        // get /users/{userSpecifier}/externalInfo
        // ctx.user.getExternalInfo();
        // 200 - The external user information has been provided

        // get /accounts/{accountID}/positions
        // ctx.position.list();
        // 200 - The Account's Positions are provided.

        // get /accounts/{accountID}/positions/{instrument}
        // ctx.position.get();
        // 200 - The Position is provided.


        // get /accounts/{accountID}/transactions
        // ctx.transaction.list();
        // 200 - The requested time range of Transaction pages are provided.

        // get /accounts/{accountID}/transactions/{transactionID}
        // ctx.transaction.get();
        // 200 - The details of the requested Transaction are provided.

        // get /accounts/{accountID}/transactions/idrange
        // ctx.transaction.range();
        // 200 - The requested time range of Transactions are provided.

        System.out.println("TEST - GET /accounts/{accountID}/openTrades");
        System.out.println("CHECK 200 - The Account's list of open Trades is provided, expecting 0 open trades.");
        try {
			Trade[] trades = ctx.trade.listOpen(accountId).execute().get_trades();
			if (trades.length > 0)
				throw new TestFailureException("Expected 0 open trades, account has "+trades.length);
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}

        System.out.println("TEST - POST /accounts/{accountID}/orders");
        System.out.println("CHECK 201 - The Order was created as specified, expecting MarketOrder creation");
        String orderId;
        String tradeId;
        try {
			CreateResponse resp = ctx.order.create(accountId).order(
						new MarketOrderRequest()
						.set_instrument(tradeableInstrument)
						.set_units(10)
					).execute();
			Transaction trans = resp.get_orderCreateTransaction();
			if (trans.get_type() != TransactionType.MARKET_ORDER)
				throw new TestFailureException("Created order type "+ trans.get_type() + " != MARKET");
			orderId = resp.get_orderCreateTransaction().get_id();
			tradeId = resp.get_orderFillTransaction().get_id();
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (CreateRequestException404 e) {
			throw new TestFailureException(e);
		} catch (CreateRequestException400 e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
        // 400 - The Order specification was invalid
                
        System.out.println("TEST - PUT /accounts/{accountID}/trades/{tradeSpecifier}/clientExtensions");
        System.out.println("CHECK 200 - The Trade's Client Extensions have been updated as requested, expecting tag and comment to match what was set.");
        try {
			TradeClientExtensionsModifyTransaction trans = ctx.trade.setClientExtensions(accountId, tradeId)
				.clientExtensions(new ClientExtensions()
					.set_comment("this is a good trade")
					.set_tag("good"))
				.execute().get_tradeClientExtensionsModifyTransaction();
			if (!trans.get_tradeClientExtensionsModify().get_tag().equals("good"))
				throw new TestFailureException("Tag "+trans.get_tradeClientExtensionsModify().get_tag()+" != good");
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (SetclientextensionsRequestException404 e) {
			throw new TestFailureException(e);
		} catch (SetclientextensionsRequestException400 e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
        // 400 - The Trade's Client Extensions cannot be modified as requested.

        System.out.println("TEST - PUT /accounts/{accountID}/trades/{tradeSpecifier}/orders");
        System.out.println("CHECK 200 - The Trade's dependent Orders have been modified as requested, expecting pending TP with matching tradeId");
        try {
			TakeProfitOrderTransaction tp = ctx.trade.setDependentOrders(accountId, tradeId)
				.takeProfit(new TakeProfitDetails()
					.set_price(2.0))
				.execute().get_takeProfitOrderTransaction();
			if (!tp.get_tradeID().equals(tradeId))
				throw new TestFailureException("Dependent tradeId "+tp.get_tradeID()+" != "+tradeId);
			
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (SetdependentordersRequestException400 e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
        // 400 - The Trade's dependent Orders cannot be modified as requested.

        // get /accounts/{accountID}/pricing
        // ctx.pricing.get();
        // 200 - Pricing information has been successfully provided.

        System.out.println("TEST - GET /accounts/{accountID}/orders/{orderSpecifier}");
        System.out.println("CHECK 200 - The details of the Order requested match the order placed, expecting FILLED MARKET order.");
        try {
			Order order = ctx.order.get(accountId, orderId).execute().get_order();
			if (order.get_type() != OrderType.MARKET)
				throw new TestFailureException("Order type "+order.get_type()+" != MARKET");
			if (order.get_state() != OrderState.FILLED)
				throw new TestFailureException("Order state not filled");
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
        
        System.out.println("TEST - GET /accounts/{accountID}/trades");
        System.out.println("CHECK 200 - The list of trades requested, expecting the previously executed order in list.");
        try {
			Trade[] trades = ctx.trade.list(accountId).execute().get_trades();
			boolean hastrade = false;
			for (Trade trade : trades) {
				if (trade.get_id().equals(tradeId)) {
					hastrade = true;
					break;
				}
			}
			if (!hastrade) {
				throw new TestFailureException("Expected tradeId not in list");
			}
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}

        System.out.println("TEST - PUT /accounts/{accountID}/trades/{tradeSpecifier}/close");
        System.out.println("CHECK 200 - The Trade has been closed as requested, expecting single close trade.");
        try {
			TradeReduce[] trades = ctx.trade.close(accountId, tradeId).execute().get_orderFillTransaction().get_tradesClosed();
			if (trades.length != 1)
				throw new TestFailureException("Expecting 1 close trade, got "+trades.length);
			if (!trades[0].get_tradeID().equals(tradeId))
				throw new TestFailureException("Closed trade "+trades[0].get_tradeID()+ " doesn't match expected "+tradeId);
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (CloseRequestException404 e) {
			throw new TestFailureException(e);
		} catch (CloseRequestException400 e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
        // 400 - The Trade cannot be closed as requested.
        // 404 - The Account or Trade does not exist

        System.out.println("TEST - GET /accounts/{accountID}/trades/{tradeSpecifier}");
        System.out.println("CHECK 200 - The details for the requested Trade is provided, expecting CLOSED state");
        try {
			Trade trade = ctx.trade.get(accountId, tradeId).execute().get_trade();
			if (trade.get_state() != TradeState.CLOSED) 
				throw new TestFailureException("Trade state "+trade.get_state()+" != CLOSED");
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}

        System.out.println("TEST - GET /accounts/{accountID}/pendingOrders");
        System.out.println("CHECK 200 - List of pending Orders for the Account, expecting 0 pending");
        
        try {
			Order[] orders = ctx.order.listPending(accountId).execute().get_orders();
			if (orders.length > 0) 
				throw new TestFailureException("Expected 0 pending orders, received "+orders.length);
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}

        System.out.println("TEST - POST /accounts/{accountID}/orders");
        System.out.println("CHECK 201 - The Order was created as specified, expecting LimitOrder creation");
        try {
			CreateResponse resp = ctx.order.create(accountId).order(
						new LimitOrderRequest()
						.set_instrument(tradeableInstrument)
						.set_units(10)
						.set_price(1.0)
					).execute();
			Transaction trans = resp.get_orderCreateTransaction();
			if (trans.get_type() != TransactionType.LIMIT_ORDER)
				throw new TestFailureException("Created order type "+ trans.get_type() + " != LIMIT_ORDER");
			orderId = resp.get_orderCreateTransaction().get_id();
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (CreateRequestException404 e) {
			throw new TestFailureException(e);
		} catch (CreateRequestException400 e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}

        System.out.println("TEST - PUT /accounts/{accountID}/orders/{orderSpecifier}");
        System.out.println("CHECK 400 - The Order specification was invalid, expecting REPLACING_ORDER_INVALID");        
        try {
			ctx.order.replace(accountId, orderId).order(
					new StopOrderRequest()
					.set_instrument(tradeableInstrument)
					.set_units(10)
					.set_price(1.0)
				).execute();
			throw new TestFailureException("Unexpected success replacing LimitOrder");
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (ReplaceRequestException404 e) {
			throw new TestFailureException(e);
		} catch (ReplaceRequestException400 e) {
			System.out.println(e.get_errorCode()+ " " +e.get_errorMessage());
			if (!e.get_errorCode().equals("REPLACING_ORDER_INVALID")) {
				throw new TestFailureException("Unexpected errorCode "+e.get_errorCode()+" in expected exception");
			}
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
        System.out.println("CHECK 201 - The Order was successfully cancelled and replaced, expecting triggered and closed LimitOrder");
		try {
			ReplaceResponse resp = ctx.order.replace(accountId, orderId).order(
					new LimitOrderRequest()
					.set_instrument(tradeableInstrument)
					.set_units(10)
					.set_price(2.0)
					.set_takeProfitOnFill(
							new TakeProfitDetails()
							.set_price(2.0)
					)
				).execute();
			OrderFillTransaction trans = resp.get_orderFillTransaction();
			double units = trans.get_tradeOpened().get_units();
			if (units != 10)
				throw new TestFailureException("Expected open trade units "+units+" != 10");
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (ReplaceRequestException404 e) {
			throw new TestFailureException(e);
		} catch (ReplaceRequestException400 e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
		
        System.out.println("TEST - GET /accounts/{accountID}/orders");
        System.out.println("CHECK 200 - The list of Orders requested, expecting 1 TakeProfit");
        try {
			Order[] orders = ctx.order.list(accountId).execute().get_orders();
			if (orders.length != 1)
				throw new TestFailureException("Expected order count "+orders.length+" != 1");
			Order order = orders[0];
			orderId = order.get_id();
			if (order.get_type() != OrderType.TAKE_PROFIT)
				throw new TestFailureException("Unexpected Order Type "+order.get_type()+" != TAKE_PROFIT");
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}

        System.out.println("TEST - PUT /accounts/{accountID}/orders/{orderSpecifier}/cancel");
        System.out.println("CHECK 200 - The Order was cancelled as specified, expecting cancelled order");
        try {
			ctx.order.cancel(accountId, orderId).execute().get_orderCancelTransaction();
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (CancelRequestException404 e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
        // 404 - The Order in the Account could not be cancelled

        // put /accounts/{accountID}/orders/{orderSpecifier}/clientExtensions
        // ctx.order.setClientExtensions();
        // 200 - The Order's Client Extensions were successfully modified
        // 400 - The Order Client Extensions specification was invalid

        System.out.println("TEST GET /accounts/{accountID}/openPositions");
        System.out.println("CHECK 200 - The Account's open Positions are provided, expecting 1 EUR/USD position of 10 units.");
        try {
			Position[] positions = ctx.position.listOpen(accountId).execute().get_positions();
			if (positions.length != 1) 
				throw new TestFailureException("Position count "+positions.length+" != 1");
			Position position = positions[0];
			if (position.get_long().get_units() != 10)
				throw new TestFailureException("Position units "+position.get_long()+" != 10");
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}

        System.out.println("TEST - PUT /accounts/{accountID}/positions/{instrument}/close");
        System.out.println("CHECK 200 - The Position closeout request has been successfully processed, expecting 1 10 unit position closed.");
        try {
			OrderFillTransaction trans = ctx.position.close(accountId, tradeableInstrument)
					.longUnits("ALL")
					.execute().get_longOrderFillTransaction();
			if (trans.get_units() != -10)
				throw new TestFailureException("Position units "+trans.get_units()+"!= -10");
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (com.oanda.v20.Context.PositionContext.CloseRequestException404 e) {
			throw new TestFailureException(e);
		} catch (com.oanda.v20.Context.PositionContext.CloseRequestException400 e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
        // 400 - The Parameters provided that describe the Position closeout are
        // invalid.

        // get /instruments/{instrument}/candles
        // ctx.instrument.candles();
        // 200 - Pricing information has been successfully provided.

        System.out.println("TEST - GET /accounts/{accountID}/transactions/sinceid");
        System.out.println("CHECK 200 - The requested time range of Transactions are provided, expecting 16 transactions.");
        try {
			Transaction[] transactions = ctx.transaction.since(accountId).id(firstTransId).execute().get_transactions();
			System.out.println("Executed a total of "+transactions.length+" transactions:");
			for (Transaction trans : transactions) {
				System.out.println(trans.get_id() + " " + trans.get_type());
			}
			if (transactions.length != 16)
				throw new TestFailureException("Number of transactions "+transactions.length+" != 16");
		} catch (ClientProtocolException e) {
			throw new TestFailureException(e);
		} catch (IOException e) {
			throw new TestFailureException(e);
		} catch (RequestException e) {
			throw new TestFailureException(e);
		}
        
		System.out.println("SUCCESS");
	}
}
