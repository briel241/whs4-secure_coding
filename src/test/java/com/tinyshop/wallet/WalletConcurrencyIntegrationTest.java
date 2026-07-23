package com.tinyshop.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tinyshop.PostgresIntegrationTest;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.user.domain.User;
import com.tinyshop.wallet.domain.WalletLedgerEntry;
import com.tinyshop.wallet.dto.TransferForm;
import com.tinyshop.wallet.repository.WalletAccountRepository;
import com.tinyshop.wallet.repository.WalletLedgerRepository;
import com.tinyshop.wallet.service.WalletService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

class WalletConcurrencyIntegrationTest extends PostgresIntegrationTest {
    private static final String PASSWORD = "wallet-password-123";
    @Autowired WalletService walletService;
    @Autowired WalletAccountRepository wallets;
    @Autowired WalletLedgerRepository ledger;
    @Autowired TransactionTemplate transactions;
    @Autowired JdbcTemplate jdbc;

    @Test
    void normalTransferHasZeroSumLedgerAndSequentialRetryDoesNotDoubleDebit() {
        User sender = register("wallet-sender", PASSWORD);
        User recipient = register("wallet-recipient", PASSWORD);
        grant(sender, 100);
        var intent = walletService.prepare(sender.getId(), form(recipient, 80));
        var first = walletService.execute(intent.getId(), sender.getId(), PASSWORD);
        var retry = walletService.execute(intent.getId(), sender.getId(), PASSWORD);
        assertThat(retry.getId()).isEqualTo(first.getId());
        assertThat(walletService.wallet(sender.getId()).getBalance()).isEqualTo(20);
        assertThat(walletService.wallet(recipient.getId()).getBalance()).isEqualTo(80);
        assertThat(ledger.sumForTransfer(first.getId())).isZero();
        assertThat(walletService.reconcile(sender.getId()).matches()).isTrue();
        assertThat(walletService.reconcile(recipient.getId()).matches()).isTrue();
    }

    @Test
    void tamperingAndInvalidIntentsAreRejected() {
        User sender = register("tamper-sender", PASSWORD);
        User recipient = register("tamper-recipient", PASSWORD);
        User outsider = register("tamper-outsider", PASSWORD);
        grant(sender, 100);
        var intent = walletService.prepare(sender.getId(), form(recipient, 50));
        assertThatThrownBy(() -> walletService.execute(intent.getId(), outsider.getId(), PASSWORD))
            .isInstanceOf(com.tinyshop.common.exception.NotFoundException.class);
        assertThatThrownBy(() -> walletService.execute(intent.getId(), sender.getId(), "wrong-password"))
            .isInstanceOf(BusinessException.class);
        assertThat(walletService.wallet(sender.getId()).getBalance()).isEqualTo(100);
    }

    @Test
    void twoConcurrentEightyPointTransfersFromHundredAllowAtMostOne() throws Exception {
        User sender = register("race-sender", PASSWORD);
        User recipientA = register("race-a", PASSWORD);
        User recipientB = register("race-b", PASSWORD);
        grant(sender, 100);
        var intentA = walletService.prepare(sender.getId(), form(recipientA, 80));
        var intentB = walletService.prepare(sender.getId(), form(recipientB, 80));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Boolean>> futures = List.of(
                executor.submit(execution(start, intentA.getId(), sender.getId())),
                executor.submit(execution(start, intentB.getId(), sender.getId())));
            start.countDown();
            long successes = 0;
            for (Future<Boolean> future : futures) if (future.get()) successes++;
            assertThat(successes).isEqualTo(1);
        }
        assertThat(walletService.wallet(sender.getId()).getBalance()).isEqualTo(20);
    }

    @Test
    void parallelRetryWithSameIntentReturnsOneTransfer() throws Exception {
        User sender = register("idem-sender", PASSWORD);
        User recipient = register("idem-recipient", PASSWORD);
        grant(sender, 100);
        var intent = walletService.prepare(sender.getId(), form(recipient, 40));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<UUID>> results = List.of(
                executor.submit(idempotentExecution(start, intent.getId(), sender.getId())),
                executor.submit(idempotentExecution(start, intent.getId(), sender.getId())));
            start.countDown();
            assertThat(results.get(0).get()).isEqualTo(results.get(1).get());
        }
        assertThat(walletService.wallet(sender.getId()).getBalance()).isEqualTo(60);
    }

    @Test
    void databaseFailureDuringLedgerInsertRollsBackEverything() {
        User sender = register("rollback-sender", PASSWORD);
        User recipient = register("rollback-recipient", PASSWORD);
        grant(sender, 100);
        var intent = walletService.prepare(sender.getId(), form(recipient, 50));
        jdbc.execute("""
            CREATE OR REPLACE FUNCTION test_fail_ledger() RETURNS trigger AS $$
            BEGIN RAISE EXCEPTION 'intentional test failure'; END;
            $$ LANGUAGE plpgsql
            """);
        jdbc.execute("""
            CREATE TRIGGER test_fail_ledger_trigger
            BEFORE INSERT ON wallet_ledger_entries
            FOR EACH ROW EXECUTE FUNCTION test_fail_ledger()
            """);
        try {
            assertThatThrownBy(() -> walletService.execute(intent.getId(), sender.getId(), PASSWORD))
                .isInstanceOf(RuntimeException.class);
        } finally {
            jdbc.execute("DROP TRIGGER IF EXISTS test_fail_ledger_trigger ON wallet_ledger_entries");
            jdbc.execute("DROP FUNCTION IF EXISTS test_fail_ledger()");
        }
        assertThat(walletService.wallet(sender.getId()).getBalance()).isEqualTo(100);
        assertThat(walletService.wallet(recipient.getId()).getBalance()).isZero();
        assertThat(walletService.history(sender.getId())).isEmpty();
    }

    @Test
    void oppositeDirectionTransfersUseDeterministicLocksWithoutDeadlock() throws Exception {
        User first = register("opposite-a", PASSWORD);
        User second = register("opposite-b", PASSWORD);
        grant(first, 100);
        grant(second, 100);
        var firstIntent = walletService.prepare(first.getId(), form(second, 10));
        var secondIntent = walletService.prepare(second.getId(), form(first, 20));
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var a = executor.submit(idempotentExecution(start, firstIntent.getId(), first.getId()));
            var b = executor.submit(idempotentExecution(start, secondIntent.getId(), second.getId()));
            start.countDown();
            assertThat(a.get()).isNotNull();
            assertThat(b.get()).isNotNull();
        }
        assertThat(walletService.wallet(first.getId()).getBalance()).isEqualTo(110);
        assertThat(walletService.wallet(second.getId()).getBalance()).isEqualTo(90);
    }

    private Callable<Boolean> execution(CountDownLatch start, UUID intent, UUID sender) {
        return () -> {
            start.await();
            try {
                walletService.execute(intent, sender, PASSWORD);
                return true;
            } catch (BusinessException exception) {
                return false;
            }
        };
    }

    private Callable<UUID> idempotentExecution(CountDownLatch start, UUID intent, UUID sender) {
        return () -> {
            start.await();
            return walletService.execute(intent, sender, PASSWORD).getId();
        };
    }

    private void grant(User user, long amount) {
        transactions.executeWithoutResult(status -> {
            var wallet = wallets.findByUserId(user.getId()).orElseThrow();
            wallet.credit(amount);
            ledger.save(WalletLedgerEntry.grant(wallet, amount));
        });
    }

    private TransferForm form(User recipient, long amount) {
        TransferForm form = new TransferForm();
        form.setRecipientNickname(recipient.getNickname());
        form.setAmount(amount);
        form.setMemo("test");
        return form;
    }
}
