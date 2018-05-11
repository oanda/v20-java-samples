package com.oanda.v20.v20sample;

import java.util.Arrays;
import java.util.List;

import com.oanda.v20.Context;
import com.oanda.v20.ContextBuilder;
import com.oanda.v20.ExecuteException;
import com.oanda.v20.RequestException;
import com.oanda.v20.account.Account;
import com.oanda.v20.account.AccountConfigure400RequestException;
import com.oanda.v20.account.AccountConfigureResponse;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.account.AccountProperties;
import com.oanda.v20.order.LimitOrderRequest;
import com.oanda.v20.order.MarketOrderRequest;
import com.oanda.v20.order.Order;
import com.oanda.v20.order.OrderCreateRequest;
import com.oanda.v20.order.OrderCreateResponse;
import com.oanda.v20.order.OrderID;
import com.oanda.v20.order.OrderReplace400RequestException;
import com.oanda.v20.order.OrderReplaceRequest;
import com.oanda.v20.order.OrderReplaceResponse;
import com.oanda.v20.order.OrderSpecifier;
import com.oanda.v20.order.OrderState;
import com.oanda.v20.order.OrderType;
import com.oanda.v20.order.StopOrderRequest;
import com.oanda.v20.position.Position;
import com.oanda.v20.position.PositionCloseRequest;
import com.oanda.v20.pricing.ClientPrice;
import com.oanda.v20.primitives.Instrument;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.trade.Trade;
import com.oanda.v20.trade.TradeCloseRequest;
import com.oanda.v20.trade.TradeSetClientExtensionsRequest;
import com.oanda.v20.trade.TradeSetDependentOrdersRequest;
import com.oanda.v20.trade.TradeSpecifier;
import com.oanda.v20.trade.TradeState;
import com.oanda.v20.transaction.ClientExtensions;
import com.oanda.v20.transaction.OrderCancelTransaction;
import com.oanda.v20.transaction.OrderFillTransaction;
import com.oanda.v20.transaction.TakeProfitDetails;
import com.oanda.v20.transaction.TakeProfitOrderTransaction;
import com.oanda.v20.transaction.TradeClientExtensionsModifyTransaction;
import com.oanda.v20.transaction.TradeReduce;
import com.oanda.v20.transaction.Transaction;
import com.oanda.v20.transaction.TransactionID;
import com.oanda.v20.transaction.TransactionType;

/**
 * This is a small sample application that demonstrates idiomatic usage of the v20 java library,
 * including chained method calls and exception handling.
 * <p>
 * For a step by step breakdown of the actions of individual requests see {@link StepByStepOrder}
 * <p>
 * @author Michael Gentili
 */
public class TestTradesAndOrders {

    Context ctx = new ContextBuilder(Config.URL)
    		.setToken(Config.TOKEN)
    		.setApplication("TestTradesAndOrders")
    		.build();
    
    AccountID accountId = Config.ACCOUNTID;
    InstrumentName tradeableInstrument = Config.INSTRUMENT;

    public static void main(String[] args) {
        try {
            new TestTradesAndOrders().runTest();
        } catch (ExecuteException | RequestException e) {
            throw new TestFailureException(e);
        }
    }

    private void runTest() throws ExecuteException, RequestException
    {
        System.out.println("TEST - GET /accounts");
        System.out.println("CHECK 200 - The list of authorized AccoungetInstrumentsts has been provided, expecting "+accountId+" in list.");

        List<AccountProperties> accountProperties = ctx.account.list().getAccounts();
        boolean hasaccount = false;
        for (AccountProperties account : accountProperties) {
            if (account.getId().equals(accountId)) {
                hasaccount = true;
                break;
            }
        }
        if (!hasaccount)
            throw new RuntimeException("Account "+accountId+" not found");

        System.out.println("TEST - GET /accounts/{accountID}");
        System.out.println("CHECK 200 - The full Account details are provided, expecting balance > 0.");
        TransactionID firstTransId;  // Store the last transaction that happened before this session

        Account account = ctx.account.get(accountId).getAccount();
        firstTransId = account.getLastTransactionID();
        if (account.getBalance().doubleValue() <= 0.0)
            throw new TestFailureException("Account "+accountId+" balance "+account.getBalance()+" <= 0");

        // get /accounts/{accountID}/summary
        // ctx.account.summary();
        // 200 - The Account summary  are provided

        System.out.println ("TEST - GET /accounts/{accountID}/instruments");
        System.out.println ("CHECK 200 - The list of tradeable instruments for the Account has been provided, expecting "+tradeableInstrument+" is tradeable.");

        List<Instrument> instruments = ctx.account.instruments(accountId).getInstruments();
        boolean istradeable = false;
        for (Instrument instrument : instruments) {
            if (instrument.getName().equals(tradeableInstrument)) {
                istradeable = true;
                break;
            }
        }
        if (!istradeable)
            throw new TestFailureException("Instrument "+tradeableInstrument+" is not tradeable");

        System.out.println("TEST - PATCH /accounts/{accountID}/configuration");
        System.out.println("CHECK 400 - The Account could not be configured successfully, expecting [must specify parameter] exception.");
        try {
            AccountConfigureResponse result = ctx.account.configure(accountId);
            throw new TestFailureException("Unexpected Success:" + result.getClientConfigureTransaction());
        } catch (AccountConfigure400RequestException e) {
            // PASS
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

        List<Trade> trades = ctx.trade.listOpen(accountId).getTrades();
        if (trades.size() > 0)
            throw new TestFailureException("Expected 0 open trades, account has "+trades.size());

        System.out.println("TEST - POST /accounts/{accountID}/orders");
        System.out.println("CHECK 201 - The Order was created as specified, expecting MarketOrder creation");
        TransactionID orderTransId;
        TransactionID tradeTransId;

        OrderCreateResponse resp = ctx.order.create(new OrderCreateRequest(accountId)
                .setOrder(new MarketOrderRequest()
                    .setInstrument(tradeableInstrument)
                    .setUnits(10)
                )
            );
        Transaction orderTrans = resp.getOrderCreateTransaction();
        if (orderTrans.getType() != TransactionType.MARKET_ORDER)
            throw new TestFailureException("Created order type "+ orderTrans.getType() + " != MARKET");
        orderTransId = resp.getOrderCreateTransaction().getId();
        tradeTransId = resp.getOrderFillTransaction().getId();

        System.out.println("TEST - PUT /accounts/{accountID}/trades/{tradeSpecifier}/clientExtensions");
        System.out.println("CHECK 200 - The Trade's Client Extensions have been updated as requested, expecting tag and comment to match what was set.");

        TradeClientExtensionsModifyTransaction trans = ctx.trade.setClientExtensions(
                new TradeSetClientExtensionsRequest(accountId, new TradeSpecifier(tradeTransId))
                    .setClientExtensions(new ClientExtensions()
                        .setComment("this is a good trade")
                        .setTag("good")
                    )
                ).getTradeClientExtensionsModifyTransaction();
        if (!trans.getTradeClientExtensionsModify().getTag().equals("good"))
            throw new TestFailureException("Tag "+trans.getTradeClientExtensionsModify().getTag()+" != good");

        System.out.println("TEST - PUT /accounts/{accountID}/trades/{tradeSpecifier}/orders");
        System.out.println("CHECK 200 - The Trade's dependent Orders have been modified as requested, expecting pending TP with matching tradeId");

        TakeProfitOrderTransaction tp = ctx.trade.setDependentOrders(
                new TradeSetDependentOrdersRequest(accountId, new TradeSpecifier(tradeTransId))
                    .setTakeProfit(new TakeProfitDetails().setPrice(2.0))
                ).getTakeProfitOrderTransaction();
        if (!tp.getTradeID().equals(tradeTransId))
            throw new TestFailureException("Dependent tradeId "+tp.getTradeID()+" != "+tradeTransId);

        System.out.println("TEST - PUT /accounts/{accountID}/trades/{tradeSpecifier}/orders");
        System.out.println("CHECK 200 - The Trade's dependent Orders have been modified as requested, expecting TP to be removed");

        OrderCancelTransaction oc = ctx.trade.setDependentOrders(
                new TradeSetDependentOrdersRequest(accountId, new TradeSpecifier(tradeTransId))
                    .setTakeProfit(null)
                ).getTakeProfitOrderCancelTransaction();
        if (!oc.getOrderID().equals(tp.getId()))
            throw new TestFailureException("Dependent orderId "+oc.getOrderID()+" != "+tp.getId());

        System.out.println("TEST - GET /accounts/{accountID}/pricing/{instruments}");
        System.out.println("CHECK 200 - Pricing information has been successfully provided.");

        String[] instrumentNames = {"USD_CAD", "GBP_USD"};
        List<ClientPrice> prices = ctx.pricing.get(accountId, Arrays.asList(instrumentNames)).getPrices();

        System.out.println("TEST - GET /accounts/{accountID}/orders/{orderSpecifier}");
        System.out.println("CHECK 200 - The details of the Order requested match the order placed, expecting FILLED MARKET order.");

        Order order = ctx.order.get(accountId, new OrderSpecifier(orderTransId)).getOrder();
        if (order.getType() != OrderType.MARKET)
            throw new TestFailureException("Order type "+order.getType()+" != MARKET");
        if (order.getState() != OrderState.FILLED)
            throw new TestFailureException("Order state not filled");

        System.out.println("TEST - GET /accounts/{accountID}/trades");
        System.out.println("CHECK 200 - The list of trades requested, expecting the previously executed order in list.");

        trades = ctx.trade.list(accountId).getTrades();
        boolean hastrade = false;
        for (Trade trade : trades) {
            if (trade.getId().equals(tradeTransId)) {
                hastrade = true;
                break;
            }
        }
        if (!hastrade) {
            throw new TestFailureException("Expected tradeId not in list");
        }

        System.out.println("TEST - PUT /accounts/{accountID}/trades/{tradeSpecifier}/close");
        System.out.println("CHECK 200 - The Trade has been closed as requested, expecting single close trade.");

        List<TradeReduce> reducedTrades = ctx.trade.close(
            new TradeCloseRequest(accountId, new TradeSpecifier(tradeTransId))
        ).getOrderFillTransaction().getTradesClosed();
        if (reducedTrades.size() != 1)
            throw new TestFailureException("Expecting 1 close trade, got "+reducedTrades.size());
        if (!reducedTrades.get(0).getTradeID().equals(tradeTransId))
            throw new TestFailureException("Closed trade "+reducedTrades.get(0).getTradeID()+ " doesn't match expected "+tradeTransId);

        System.out.println("TEST - GET /accounts/{accountID}/trades/{tradeSpecifier}");
        System.out.println("CHECK 200 - The details for the requested Trade is provided, expecting CLOSED state");

        Trade trade = ctx.trade.get(accountId, new TradeSpecifier(tradeTransId)).getTrade();
        if (trade.getState() != TradeState.CLOSED)
            throw new TestFailureException("Trade state "+trade.getState()+" != CLOSED");

        System.out.println("TEST - GET /accounts/{accountID}/pendingOrders");
        System.out.println("CHECK 200 - List of pending Orders for the Account, expecting 0 pending");

        List<Order> orders = ctx.order.listPending(accountId).getOrders();
        if (orders.size() > 0)
            throw new TestFailureException("Expected 0 pending orders, received "+orders.size());

        System.out.println("TEST - POST /accounts/{accountID}/orders");
        System.out.println("CHECK 201 - The Order was created as specified, expecting LimitOrder creation");

        resp = ctx.order.create(new OrderCreateRequest(accountId)
                .setOrder(
                    new LimitOrderRequest()
                        .setInstrument(tradeableInstrument)
                        .setUnits(10)
                        .setPrice(1.0)
                ));
        orderTrans = resp.getOrderCreateTransaction();
        if (orderTrans.getType() != TransactionType.LIMIT_ORDER)
            throw new TestFailureException("Created order type "+ orderTrans.getType() + " != LIMIT_ORDER");
        orderTransId = resp.getOrderCreateTransaction().getId();

        System.out.println("TEST - PUT /accounts/{accountID}/orders/{orderSpecifier}");
        System.out.println("CHECK 400 - The Order specification was invalid, expecting REPLACING_ORDER_INVALID");

        try {
            ctx.order.replace(new OrderReplaceRequest(accountId, new OrderSpecifier(orderTransId))
                    .setOrder(
                        new StopOrderRequest()
                            .setInstrument(tradeableInstrument)
                            .setUnits(10)
                            .setPrice(1.0)
                    )
                );
            throw new TestFailureException("Unexpected success replacing LimitOrder");
        } catch (OrderReplace400RequestException e) {
            System.out.println(e.getErrorCode()+ " " +e.getErrorMessage());
            if (!e.getErrorCode().equals("REPLACING_ORDER_INVALID")) {
                throw new TestFailureException("Unexpected errorCode "+e.getErrorCode()+" in expected exception");
            }
        }

        System.out.println("CHECK 201 - The Order was successfully cancelled and replaced, expecting triggered and closed LimitOrder");

        OrderReplaceResponse replaceResp = ctx.order.replace(
            new OrderReplaceRequest(accountId, new OrderSpecifier(orderTransId))
                .setOrder(
                    new LimitOrderRequest()
                        .setInstrument(tradeableInstrument)
                        .setUnits(10)
                        .setPrice(2.0)
                        .setTakeProfitOnFill(
                            new TakeProfitDetails()
                                .setPrice(2.0)
                        )
                )
            );
        OrderFillTransaction fillTrans = replaceResp.getOrderFillTransaction();
        double units = fillTrans.getTradeOpened().getUnits().doubleValue();
        if (units != 10)
            throw new TestFailureException("Expected open trade units "+units+" != 10");

        System.out.println("TEST - GET /accounts/{accountID}/orders");
        System.out.println("CHECK 200 - The list of Orders requested, expecting 1 TakeProfit");

        orders = ctx.order.list(accountId).getOrders();
        if (orders.size() != 1)
            throw new TestFailureException("Expected order count "+orders.size()+" != 1");
        order = orders.get(0);
        OrderID orderId = order.getId();
        if (order.getType() != OrderType.TAKE_PROFIT)
            throw new TestFailureException("Unexpected Order Type "+order.getType()+" != TAKE_PROFIT");

        System.out.println("TEST - PUT /accounts/{accountID}/orders/{orderSpecifier}/cancel");
        System.out.println("CHECK 200 - The Order was cancelled as specified, expecting cancelled order");

        ctx.order.cancel(accountId, new OrderSpecifier(orderId)).getOrderCancelTransaction();

        // 404 - The Order in the Account could not be cancelled

        // put /accounts/{accountID}/orders/{orderSpecifier}/clientExtensions
        // ctx.order.setClientExtensions();
        // 200 - The Order's Client Extensions were successfully modified
        // 400 - The Order Client Extensions specification was invalid

        System.out.println("TEST GET /accounts/{accountID}/openPositions");
        System.out.println("CHECK 200 - The Account's open Positions are provided, expecting 1 EUR/USD position of 10 units.");

        List<Position> positions = ctx.position.listOpen(accountId).getPositions();
        if (positions.size() != 1)
            throw new TestFailureException("Position count "+positions.size()+" != 1");
        Position position = positions.get(0);
        if (position.getLong().getUnits().doubleValue() != 10)
            throw new TestFailureException("Position units "+position.getLong()+" != 10");

        System.out.println("TEST - PUT /accounts/{accountID}/positions/{instrument}/close");
        System.out.println("CHECK 200 - The Position closeout request has been successfully processed, expecting 1 10 unit position closed.");

        fillTrans = ctx.position.close(new PositionCloseRequest(accountId, tradeableInstrument)
                .setLongUnits("ALL")
            ).getLongOrderFillTransaction();
        if (fillTrans.getUnits().doubleValue() != -10)
            throw new TestFailureException("Position units "+fillTrans.getUnits()+"!= -10");

        // 400 - The Parameters provided that describe the Position closeout are
        // invalid.

        // get /instruments/{instrument}/candles
        // ctx.instrument.candles();
        // 200 - Pricing information has been successfully provided.

        System.out.println("TEST - GET /accounts/{accountID}/transactions/sinceid");
        System.out.println("CHECK 200 - The requested time range of Transactions are provided, expecting 16 transactions.");

        List<Transaction> transactions = ctx.transaction.since(accountId, firstTransId).getTransactions();
        System.out.println("Executed a total of "+transactions.size()+" transactions:");
        for (Transaction t : transactions) {
            System.out.println(t.getId() + " " + t.getType());
        }
        if (transactions.size() != 16)
            throw new TestFailureException("Number of transactions "+transactions.size()+" != 16");

        System.out.println("SUCCESS");
    }
}
