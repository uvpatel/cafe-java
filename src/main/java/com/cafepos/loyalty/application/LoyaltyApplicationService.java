package com.cafepos.loyalty.application;

import com.cafepos.loyalty.domain.LoyaltyAccount;
import com.cafepos.loyalty.domain.LoyaltyAccountRepository;
import com.cafepos.loyalty.domain.LoyaltyTransaction;
import com.cafepos.loyalty.domain.LoyaltyTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class LoyaltyApplicationService {

    private final LoyaltyAccountRepository accountRepository;
    private final LoyaltyTransactionRepository transactionRepository;

    public LoyaltyApplicationService(LoyaltyAccountRepository accountRepository, LoyaltyTransactionRepository transactionRepository) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    public LoyaltyAccount getOrCreateAccount(UUID cafeId, UUID customerId) {
        return accountRepository.findByCafeIdAndCustomerId(cafeId, customerId)
                .orElseGet(() -> accountRepository.save(new LoyaltyAccount(cafeId, customerId)));
    }

    public LoyaltyAccount earnPoints(UUID cafeId, UUID customerId, int points, UUID referenceOrderId, String description) {
        LoyaltyAccount account = getOrCreateAccount(cafeId, customerId);
        account.addPoints(points);
        LoyaltyAccount updatedAccount = accountRepository.save(account);

        LoyaltyTransaction transaction = new LoyaltyTransaction(
                cafeId, updatedAccount.getId(), LoyaltyTransaction.TransactionType.EARNED,
                points, updatedAccount.getCurrentPoints(), referenceOrderId, description
        );
        transactionRepository.save(transaction);

        return updatedAccount;
    }

    public LoyaltyAccount redeemPoints(UUID cafeId, UUID customerId, int points, UUID referenceOrderId, String description) {
        LoyaltyAccount account = accountRepository.findByCafeIdAndCustomerId(cafeId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Loyalty account not found for customer: " + customerId));

        account.redeemPoints(points);
        LoyaltyAccount updatedAccount = accountRepository.save(account);

        LoyaltyTransaction transaction = new LoyaltyTransaction(
                cafeId, updatedAccount.getId(), LoyaltyTransaction.TransactionType.REDEEMED,
                -points, updatedAccount.getCurrentPoints(), referenceOrderId, description
        );
        transactionRepository.save(transaction);

        return updatedAccount;
    }

    @Transactional(readOnly = true)
    public List<LoyaltyTransaction> getTransactionHistory(UUID cafeId, UUID customerId) {
        LoyaltyAccount account = accountRepository.findByCafeIdAndCustomerId(cafeId, customerId)
                .orElseThrow(() -> new IllegalArgumentException("Loyalty account not found for customer: " + customerId));
        return transactionRepository.findByCafeIdAndLoyaltyAccountIdOrderByCreatedAtDesc(cafeId, account.getId());
    }
}
