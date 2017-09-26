package com.oanda.v20.v20sample;

import java.util.HashMap;
import java.util.Map;

import com.oanda.v20.Context;
import com.oanda.v20.account.Account;
import com.oanda.v20.account.AccountChanges;
import com.oanda.v20.account.AccountChangesRequest;
import com.oanda.v20.account.AccountChangesResponse;
import com.oanda.v20.account.AccountChangesState;
import com.oanda.v20.account.AccountGetResponse;
import com.oanda.v20.account.AccountID;
import com.oanda.v20.order.DynamicOrderState;
import com.oanda.v20.order.Order;
import com.oanda.v20.order.OrderID;
import com.oanda.v20.order.TrailingStopLossOrder;
import com.oanda.v20.position.CalculatedPositionState;
import com.oanda.v20.position.Position;
import com.oanda.v20.primitives.InstrumentName;
import com.oanda.v20.trade.CalculatedTradeState;
import com.oanda.v20.trade.TradeID;
import com.oanda.v20.trade.TradeSummary;
import com.oanda.v20.transaction.TransactionID;

public class AccountUpdateLoop {

    public static void main(String[] args) {
        Context ctx = new Context(Config.URL, Config.TOKEN);
        AccountID accountId = Config.ACCOUNTID;

        // Get initial account state
        try {
            AccountGetResponse accountStateResponse = ctx.account.get(accountId);
            Account account = accountStateResponse.getAccount();
            TransactionID lastTransactionId = accountStateResponse.getLastTransactionID();

            while (true) {
                System.out.println("Polling from "+lastTransactionId);
                AccountChangesResponse resp = ctx.account.changes(
                        new AccountChangesRequest(accountId)
                            .setSinceTransactionID(lastTransactionId)
                        );
                lastTransactionId = resp.getLastTransactionID();

                AccountChanges changes = resp.getChanges();
                applyAccountChanges(account, changes);

                AccountChangesState updatedstate = resp.getState();
                applyAccountChangesState(account, updatedstate);
                Thread.sleep(1000);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void applyAccountChanges(
            Account account, AccountChanges changes
    ) {
        System.out.println("Account Changes:");
        System.out.println(changes);

        Map<OrderID, Order> ordermap = new HashMap<>();

        for (Order order : account.getOrders())
            ordermap.put(order.getId(), order);

        for (Order created : changes.getOrdersCreated())
            ordermap.put(created.getId(), created);
        for (Order cancelled : changes.getOrdersCancelled())
            ordermap.remove(cancelled.getId());
        for (Order filled : changes.getOrdersFilled())
            ordermap.remove(filled.getId());
        for (Order triggered : changes.getOrdersTriggered())
            ordermap.remove(triggered.getId());

        account.setOrders(ordermap.values());

        Map<TradeID, TradeSummary> trademap = new HashMap<>();

        for (TradeSummary trade : account.getTrades())
            trademap.put(trade.getId(), trade);

        for (TradeSummary opened : changes.getTradesOpened())
            trademap.put(opened.getId(), opened);
        for (TradeSummary reduced : changes.getTradesReduced())
            trademap.put(reduced.getId(),reduced);
        for (TradeSummary closed : changes.getTradesClosed())
            trademap.remove(closed.getId());

        account.setTrades(trademap.values());

        Map<InstrumentName, Position> positionMap = new HashMap<>();

        for (Position position : account.getPositions())
            positionMap.put(position.getInstrument(), position);

        for (Position position : changes.getPositions())
            positionMap.put(position.getInstrument(), position);

        account.setPositions(positionMap.values());
    }

    private static void applyAccountChangesState(
            Account account, AccountChangesState updatedstate
    ) {
        System.out.println("Account Changes State:");
        System.out.println(updatedstate);

        if (updatedstate.getUnrealizedPL() != null)
            account.setUnrealizedPL(updatedstate.getUnrealizedPL());
        if (updatedstate.getNAV() != null)
            account.setNAV(updatedstate.getNAV());
        if (updatedstate.getMarginUsed() != null)
            account.setMarginUsed(updatedstate.getMarginUsed());
        if (updatedstate.getMarginAvailable() != null)
            account.setMarginAvailable(updatedstate.getMarginAvailable());
        if (updatedstate.getPositionValue() != null)
            account.setPositionValue(updatedstate.getPositionValue());
        if (updatedstate.getMarginCloseoutUnrealizedPL() != null)
            account.setMarginCloseoutUnrealizedPL(
                updatedstate.getMarginCloseoutUnrealizedPL()
            );
        if (updatedstate.getMarginCloseoutNAV() != null)
            account.setMarginCloseoutNAV(updatedstate.getMarginCloseoutNAV());
        if (updatedstate.getMarginCloseoutMarginUsed() != null)
            account.setMarginCloseoutMarginUsed(
                updatedstate.getMarginCloseoutMarginUsed()
            );
        if (updatedstate.getMarginCloseoutPercent() != null)
            account.setMarginCloseoutPercent(
                updatedstate.getMarginCloseoutPercent()
            );
        if (updatedstate.getMarginCloseoutPositionValue() != null)
            account.setMarginCloseoutPositionValue(
                updatedstate.getMarginCloseoutPositionValue()
            );
        if (updatedstate.getWithdrawalLimit() != null)
            account.setWithdrawalLimit(updatedstate.getWithdrawalLimit());
        if (updatedstate.getMarginCallMarginUsed() != null)
            account.setMarginCallMarginUsed(
                updatedstate.getMarginCallMarginUsed()
            );
        if (updatedstate.getMarginCallPercent() != null)
            account.setMarginCallPercent(updatedstate.getMarginCallPercent());

        Map<OrderID, Order> ordermap = new HashMap<>();

        for (Order order : account.getOrders())
            ordermap.put(order.getId(), order);
        for (DynamicOrderState orderstate : updatedstate.getOrders()) {
            TrailingStopLossOrder order = (TrailingStopLossOrder) ordermap.get(
                orderstate.getId()
            );
            order.setTrailingStopValue(orderstate.getTrailingStopValue());
        }

        Map<TradeID, TradeSummary> trademap = new HashMap<>();

        for (TradeSummary trade : account.getTrades())
            trademap.put(trade.getId(), trade);
        for (CalculatedTradeState tradestate : updatedstate.getTrades()) {
            TradeSummary trade = trademap.get(tradestate.getId());
            trade.setUnrealizedPL(tradestate.getUnrealizedPL());
        }

        Map<InstrumentName, Position> posmap = new HashMap<>();

        for (Position pos : account.getPositions())
            posmap.put(pos.getInstrument(),pos);
        for (CalculatedPositionState posstate : updatedstate.getPositions()) {
            Position pos = posmap.get(posstate.getInstrument());
            pos.setInstrument(posstate.getInstrument());
            pos.setUnrealizedPL(posstate.getNetUnrealizedPL());
            pos.getLong().setUnrealizedPL(posstate.getLongUnrealizedPL());
            pos.getShort().setUnrealizedPL(posstate.getShortUnrealizedPL());
        }
    }

}
