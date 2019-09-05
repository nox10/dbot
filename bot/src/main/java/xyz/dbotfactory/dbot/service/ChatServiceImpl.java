package xyz.dbotfactory.dbot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import xyz.dbotfactory.dbot.model.*;
import xyz.dbotfactory.dbot.repo.ChatRepository;

import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;


@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;

    private final CalcService calcService;

    @Autowired
    public ChatServiceImpl(ChatRepository chatRepository, CalcService calcService) {
        this.chatRepository = chatRepository;
        this.calcService = calcService;
    }

    @Override
    public Chat findOrCreateChatByTelegramId(long chatId) {
        Chat chat = chatRepository.findFirstByTelegramChatId(chatId);
        if (chat == null) {
            Chat newChat = Chat.builder()
                    .telegramChatId(chatId)
                    .chatState(ChatState.NO_ACTIVE_RECEIPT)
                    .receipts(new ArrayList<>())
                    .build();
            newChat = chatRepository.save(newChat);
            return newChat;
        } else {
            return chat;
        }
    }

    @Override
    public Receipt getActiveReceipt(Chat chat) {
        return chat.getReceipts()
                .stream()
                .filter(Receipt::isActive)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("no active receipt found"));
    }

    @Override
    public List<BalanceStatus> getTotalBalanceStatuses(Chat chat) {
        Stream<BalanceStatus> payments = chat.getReceipts()
                .stream()
                .flatMap(x -> getPaymentsFromReceiptAsBalanceStatuses(x).stream());

        Stream<BalanceStatus> spendings = chat.getReceipts()
                .stream()
                .flatMap(x -> getSpendingsFromReceiptAsBalanceStatuses(x).stream());


        List<BalanceStatus> allBalances = Stream.concat(payments, spendings).collect(toList());

        return calcService.getTotalBalance(allBalances);
    }

    @Override
    public List<BalanceStatus> getCurrentReceiptBalanceStatuses(Receipt receipt) {
        Stream<BalanceStatus> payments = getPaymentsFromReceiptAsBalanceStatuses(receipt).stream();

        Stream<BalanceStatus> spendings = getSpendingsFromReceiptAsBalanceStatuses(receipt).stream();

        List<BalanceStatus> allBalances = Stream.concat(payments, spendings).collect(toList());

        return calcService.getTotalBalance(allBalances);
    }

    @Override
    public List<DebtReturnTransaction> getReturnStrategy(Chat chat) {
        List<BalanceStatus> totalBalanceStatuses = getTotalBalanceStatuses(chat);
        return calcService.getReturnStrategy(totalBalanceStatuses);
    }

    private List<BalanceStatus> getSpendingsFromReceiptAsBalanceStatuses(Receipt receipt) {
        return receipt
                .getItems()
                .stream()
                .flatMap(x -> getSpendingsFromReceiptItem(x).stream())
                .collect(toList());
    }

    private List<BalanceStatus> getSpendingsFromReceiptItem(ReceiptItem item) {
        return item
                .getShares()
                .stream()
                .map(share -> BalanceStatus
                        .builder()
                        .id(share.getTelegramUserId())
                        .amount(share.getShare().multiply(item.getPrice()).negate())
                        .build())
                .collect(toList());
    }

    private List<BalanceStatus> getPaymentsFromReceiptAsBalanceStatuses(Receipt receipt) {
        return receipt
                .getUserBalances()
                .stream()
                .map(x -> BalanceStatus
                        .builder()
                        .id(x.getTelegramUserId())
                        .amount(x.getBalance())
                        .build())
                .collect(toList());
    }

    @Override
    public void save(Chat chat) {
        chatRepository.save(chat);
    }

    @Override
    public void removeActiveReceipt(Chat chat) {
        Receipt activeReceipt = getActiveReceipt(chat);
        chat.getReceipts().remove(activeReceipt);
    }
}
