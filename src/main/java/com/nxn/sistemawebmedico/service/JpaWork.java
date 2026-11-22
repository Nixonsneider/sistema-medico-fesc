package com.nxn.sistemawebmedico.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.Objects;
import java.util.function.Function;

/** Cada operación usa su propio EntityManager; las escrituras son atómicas. */
final class JpaWork {
    private final EntityManagerFactory factory;

    JpaWork(EntityManagerFactory factory) { this.factory = Objects.requireNonNull(factory); }

    <T> T read(Function<EntityManager, T> operation) {
        try (EntityManager manager = factory.createEntityManager()) { return operation.apply(manager); }
    }

    <T> T write(Function<EntityManager, T> operation) {
        try (EntityManager manager = factory.createEntityManager()) {
            var transaction = manager.getTransaction();
            transaction.begin();
            try {
                T result = operation.apply(manager);
                transaction.commit();
                return result;
            } catch (RuntimeException exception) {
                if (transaction.isActive()) transaction.rollback();
                throw exception;
            }
        }
    }
}
