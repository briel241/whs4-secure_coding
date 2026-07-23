package com.tinyshop.wallet.service;

import com.tinyshop.auth.service.IdentityNormalizer;
import com.tinyshop.common.exception.BusinessException;
import com.tinyshop.common.exception.NotFoundException;
import com.tinyshop.user.domain.User;
import com.tinyshop.user.domain.UserStatus;
import com.tinyshop.user.repository.UserRepository;
import com.tinyshop.user.service.UserService;
import com.tinyshop.wallet.domain.LedgerEntryType;
import com.tinyshop.wallet.domain.Transfer;
import com.tinyshop.wallet.domain.TransferIntent;
import com.tinyshop.wallet.domain.TransferIntentStatus;
import com.tinyshop.wallet.domain.TransferStatus;
import com.tinyshop.wallet.domain.WalletAccount;
import com.tinyshop.wallet.domain.WalletLedgerEntry;
import com.tinyshop.wallet.domain.WalletStatus;
import com.tinyshop.wallet.dto.TransferForm;
import com.tinyshop.wallet.repository.TransferIntentRepository;
import com.tinyshop.wallet.repository.TransferRepository;
import com.tinyshop.wallet.repository.WalletAccountRepository;
import com.tinyshop.wallet.repository.WalletLedgerRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WalletService {
    private static final long PER_TRANSFER_LIMIT = 1_000_000;
    private static final long DAILY_LIMIT = 5_000_000;
    private final WalletAccountRepository wallets;
    private final TransferIntentRepository intents;
    private final TransferRepository transfers;
    private final WalletLedgerRepository ledger;
    private final UserRepository userRepository;
    private final UserService users;
    private final PasswordEncoder passwordEncoder;

    public WalletService(WalletAccountRepository wallets, TransferIntentRepository intents,
                         TransferRepository transfers, WalletLedgerRepository ledger,
                         UserRepository userRepository, UserService users,
                         PasswordEncoder passwordEncoder) {
        this.wallets = wallets;
        this.intents = intents;
        this.transfers = transfers;
        this.ledger = ledger;
        this.userRepository = userRepository;
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public WalletAccount wallet(UUID userId) {
        return wallets.findByUserId(userId).orElseThrow(() -> new NotFoundException("Wallet not found"));
    }

    @Transactional
    public TransferIntent prepare(UUID senderId, TransferForm form) {
        User sender = users.require(senderId);
        User recipient = userRepository.findByNormalizedNickname(
                IdentityNormalizer.nickname(form.getRecipientNickname()))
            .orElseThrow(() -> new BusinessException("받는 사용자를 찾을 수 없습니다."));
        validateParties(sender, recipient, form.getAmount());
        WalletAccount senderWallet = wallet(senderId);
        WalletAccount recipientWallet = wallet(recipient.getId());
        validateWallet(senderWallet);
        validateWallet(recipientWallet);
        if (senderWallet.getBalance() < form.getAmount()) throw new BusinessException("잔액이 부족합니다.");
        validateDailyLimit(senderId, form.getAmount());
        String memo = form.getMemo() == null ? null : form.getMemo().strip();
        return intents.save(new TransferIntent(sender, recipient, form.getAmount(), memo));
    }

    @Transactional(readOnly = true)
    public TransferIntent pending(UUID intentId, UUID senderId) {
        TransferIntent intent = intents.findByIdAndSenderId(intentId, senderId)
            .orElseThrow(() -> new NotFoundException("Transfer intent not found"));
        if (intent.getStatus() != TransferIntentStatus.PENDING || intent.isExpired()) {
            throw new BusinessException("송금 요청이 만료되었거나 이미 사용되었습니다.");
        }
        return intent;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Transfer execute(UUID intentId, UUID senderId, String currentPassword) {
        TransferIntent initial = intents.findByIdAndSenderId(intentId, senderId)
            .orElseThrow(() -> new NotFoundException("Transfer intent not found"));
        Transfer existing = transfers.findBySenderIdAndIdempotencyKey(senderId, initial.getIdempotencyKey())
            .orElse(null);
        if (existing != null) return existing;

        TransferIntent intent = intents.lockOwned(intentId, senderId)
            .orElseThrow(() -> new NotFoundException("Transfer intent not found"));
        existing = transfers.findBySenderIdAndIdempotencyKey(senderId, intent.getIdempotencyKey()).orElse(null);
        if (existing != null) return existing;
        if (intent.getStatus() != TransferIntentStatus.PENDING) {
            throw new BusinessException("이미 처리된 송금 요청입니다.");
        }
        if (intent.isExpired()) {
            intent.markExpired();
            throw new BusinessException("송금 요청이 만료되었습니다. 다시 준비해 주세요.");
        }
        if (!passwordEncoder.matches(currentPassword, intent.getSender().getPasswordHash())) {
            throw new BusinessException("현재 비밀번호가 올바르지 않습니다.");
        }

        List<UUID> userIds = List.of(intent.getSender().getId(), intent.getRecipient().getId())
            .stream().sorted().toList();
        Map<UUID, WalletAccount> locked = wallets.lockAllByUserIds(userIds).stream()
            .collect(Collectors.toMap(account -> account.getUser().getId(), Function.identity()));
        if (locked.size() != 2) throw new BusinessException("송금 지갑을 사용할 수 없습니다.");
        WalletAccount senderWallet = locked.get(intent.getSender().getId());
        WalletAccount recipientWallet = locked.get(intent.getRecipient().getId());
        validateParties(senderWallet.getUser(), recipientWallet.getUser(), intent.getAmount());
        validateWallet(senderWallet);
        validateWallet(recipientWallet);
        validateDailyLimit(senderId, intent.getAmount());
        if (senderWallet.getBalance() < intent.getAmount()) throw new BusinessException("잔액이 부족합니다.");

        senderWallet.debit(intent.getAmount());
        recipientWallet.credit(intent.getAmount());
        Transfer transfer = transfers.save(new Transfer(intent));
        ledger.save(new WalletLedgerEntry(senderWallet, transfer, -intent.getAmount(),
            senderWallet.getBalance(), LedgerEntryType.DEBIT));
        ledger.save(new WalletLedgerEntry(recipientWallet, transfer, intent.getAmount(),
            recipientWallet.getBalance(), LedgerEntryType.CREDIT));
        intent.markUsed();
        return transfer;
    }

    @Transactional(readOnly = true)
    public List<Transfer> history(UUID userId) {
        return transfers.findHistory(userId);
    }

    @Transactional(readOnly = true)
    public Transfer receipt(UUID transferId, UUID userId) {
        return transfers.findVisible(transferId, userId)
            .orElseThrow(() -> new NotFoundException("Transfer not found"));
    }

    @Transactional(readOnly = true)
    public Reconciliation reconcile(UUID userId) {
        WalletAccount wallet = wallet(userId);
        long ledgerSum = ledger.sumForWallet(wallet.getId());
        return new Reconciliation(wallet.getBalance(), ledgerSum, wallet.getBalance() == ledgerSum);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Transfer> adminTransfers(int page) {
        return transfers.findAllByOrderByCompletedAtDesc(
            org.springframework.data.domain.PageRequest.of(Math.max(0, page), 50));
    }

    private void validateParties(User sender, User recipient, long amount) {
        if (sender.getId().equals(recipient.getId())) throw new BusinessException("자기 자신에게 송금할 수 없습니다.");
        if (amount < 1 || amount > PER_TRANSFER_LIMIT) {
            throw new BusinessException("송금액은 1~1,000,000포인트여야 합니다.");
        }
        if (sender.getStatus() != UserStatus.ACTIVE || recipient.getStatus() != UserStatus.ACTIVE) {
            throw new BusinessException("현재 계정 상태에서는 송금할 수 없습니다.");
        }
    }

    private void validateWallet(WalletAccount wallet) {
        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new BusinessException("현재 지갑 상태에서는 송금할 수 없습니다.");
        }
    }

    private void validateDailyLimit(UUID senderId, long amount) {
        Instant from = LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC);
        long sent = transfers.sumSent(senderId, TransferStatus.COMPLETED, from, from.plusSeconds(86400));
        if (sent > DAILY_LIMIT - amount) throw new BusinessException("일일 송금 한도 5,000,000포인트를 초과합니다.");
    }

    public record Reconciliation(long balance, long ledgerSum, boolean matches) {
    }
}
