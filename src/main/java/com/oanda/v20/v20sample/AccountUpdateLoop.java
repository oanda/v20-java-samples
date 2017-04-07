package com.oanda.v20.v20sample;

import java.util.HashMap;

import com.oanda.v20.Account;
import com.oanda.v20.AccountChanges;
import com.oanda.v20.AccountChangesState;
import com.oanda.v20.CalculatedPositionState;
import com.oanda.v20.CalculatedTradeState;
import com.oanda.v20.Context;
import com.oanda.v20.DynamicOrderState;
import com.oanda.v20.Order;
import com.oanda.v20.Position;
import com.oanda.v20.Trade;
import com.oanda.v20.TradeSummary;
import com.oanda.v20.TrailingStopLossOrder;
import com.oanda.v20.Context.AccountContext.ChangesRequest;
import com.oanda.v20.Context.AccountContext.ChangesResponse;
import com.oanda.v20.Context.AccountContext.GetResponse;
import com.oanda.v20.TransactionID;

public class AccountUpdateLoop {

	public static void main(String[] args) {
		Context ctx = new Context(Config.url, Config.token);
        String accountId = Config.accountId;
        String tradeableInstrument = Config.instrument;

        // Get initial account state
        try {
			GetResponse accountStateResponse = ctx.account.get(accountId).execute();
			Account account = accountStateResponse.getAccount();
			String lastTransactionId = accountStateResponse.getLastTransactionID().getTransactionID();
			
			while (true) {
				System.out.println("Polling from "+lastTransactionId);
				ChangesResponse resp = ctx.account.changes(accountId)
						.sinceTransactionID(lastTransactionId)
						.execute();
				lastTransactionId = resp.getLastTransactionID().getTransactionID();
				
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

	private static void applyAccountChanges(Account account,
			AccountChanges changes) {
		HashMap<String, Order> ordermap = new HashMap<String, Order>();
		for (Order order : account.getOrders())
			ordermap.put(order.getId(), order);
		for (Order cancelled : changes.getOrdersCancelled())
			ordermap.remove(cancelled.getId());
		for (Order filled : changes.getOrdersFilled())
			ordermap.remove(filled.getId());
		for (Order created : changes.getOrdersCreated())
			ordermap.put(created.getId(), created);
		for (Order triggered : changes.getOrdersTriggered())
			ordermap.remove(triggered.getId());
		account.setOrders(ordermap.values().toArray(new Order[0]));
		
		HashMap<String, TradeSummary> trademap = new HashMap<String, TradeSummary>();
		for (TradeSummary trade : account.getTrades())
			trademap.put(trade.getId(), trade);
		for (TradeSummary closed : changes.getTradesClosed())
			trademap.remove(closed.getId());
		for (TradeSummary opened : changes.getTradesOpened())
			trademap.put(opened.getId(), opened);
		for (TradeSummary reduced : changes.getTradesReduced())
			trademap.put(reduced.getId(),reduced);
			
		HashMap<String, Position> posmap = new HashMap<String, Position>();
		for (Position pos : account.getPositions())
			posmap.put(pos.getInstrument(), pos);
	}

	private static void applyAccountChangesState(Account account,
			AccountChangesState updatedstate) {
		account.setUnrealizedPL(updatedstate.getUnrealizedPL());		        
		account.setNAV(updatedstate.getNAV());
		account.setMarginUsed(updatedstate.getMarginUsed());
		account.setMarginAvailable(updatedstate.getMarginAvailable());
		account.setPositionValue(updatedstate.getPositionValue());
		account.setMarginCloseoutUnrealizedPL(updatedstate.getMarginCloseoutUnrealizedPL());
		account.setMarginCloseoutNAV(updatedstate.getMarginCloseoutNAV());
		account.setMarginCloseoutMarginUsed(updatedstate.getMarginCloseoutMarginUsed());
		account.setMarginCloseoutPercent(updatedstate.getMarginCloseoutPercent());
		account.setMarginCloseoutPositionValue(updatedstate.getMarginCloseoutPositionValue());
		account.setWithdrawalLimit(updatedstate.getWithdrawalLimit());
		account.setMarginCallMarginUsed(updatedstate.getMarginCallMarginUsed());
		account.setMarginCallPercent(updatedstate.getMarginCallPercent());
		
		HashMap<String, Order> ordermap = new HashMap<String, Order>();
		for (Order order : account.getOrders())
		    ordermap.put(order.getId(), order);
		for (DynamicOrderState orderstate : updatedstate.getOrders()) {
		    TrailingStopLossOrder order = (TrailingStopLossOrder) ordermap.get(orderstate.getId());
		    order.setTrailingStopValue(orderstate.getTrailingStopValue());
		}
		
		HashMap<String, TradeSummary> trademap = new HashMap<String, TradeSummary>();
		for (TradeSummary trade : account.getTrades())
			trademap.put(trade.getId(), trade);
		for (CalculatedTradeState tradestate : updatedstate.getTrades()) {
			TradeSummary trade = trademap.get(tradestate.getId());
		    trade.setUnrealizedPL(tradestate.getUnrealizedPL());
		}
		
		HashMap<String, Position> posmap = new HashMap<String, Position>();
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
