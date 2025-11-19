package com.jpmc.midascore.component;

import com.jpmc.midascore.entity.TransactionRecord;
import com.jpmc.midascore.entity.UserRecord;
import com.jpmc.midascore.foundation.Transaction;
import com.jpmc.midascore.repository.TransactionRepository;
import com.jpmc.midascore.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionListener {
    private static final Logger logger = LoggerFactory.getLogger(TransactionListener.class);

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;

    public TransactionListener(UserRepository userRepository, TransactionRepository transactionRepository) {
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @KafkaListener(topics = "${general.kafka-topic}", groupId = "midas-core-group")
    @Transactional
    public void receiveTransaction(Transaction transaction) {
        logger.info("Processing transaction: {}", transaction);

        // Validate transaction
        UserRecord sender = userRepository.findById(transaction.getSenderId());
        UserRecord recipient = userRepository.findById(transaction.getRecipientId());

        if (sender == null) {
            logger.warn("Invalid sender ID: {}", transaction.getSenderId());
            return;
        }

        if (recipient == null) {
            logger.warn("Invalid recipient ID: {}", transaction.getRecipientId());
            return;
        }

        if (sender.getBalance() < transaction.getAmount()) {
            logger.warn("Insufficient balance. Sender: {}, Balance: {}, Amount: {}", 
                       sender.getName(), sender.getBalance(), transaction.getAmount());
            return;
        }

        // Process valid transaction
        try {
            // Update balances
            sender.setBalance(sender.getBalance() - transaction.getAmount());
            recipient.setBalance(recipient.getBalance() + transaction.getAmount());

            // Save updated users
            userRepository.save(sender);
            userRepository.save(recipient);

            // Create and save transaction record
            TransactionRecord transactionRecord = new TransactionRecord(sender, recipient, transaction.getAmount());
            transactionRepository.save(transactionRecord);

            logger.info("Transaction processed successfully. Sender new balance: {}, Recipient new balance: {}", 
                       sender.getBalance(), recipient.getBalance());

        } catch (Exception e) {
            logger.error("Error processing transaction: {}", e.getMessage());
        }
    }
}