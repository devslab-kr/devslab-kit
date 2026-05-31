package com.example.consumer;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * A consumer-defined entity in the consumer's own package. Its presence as a
 * managed type proves the kit's entity auto-registration is <em>additive</em> —
 * it must not suppress the consumer's own {@code @Entity} scanning.
 */
@Entity
@Table(name = "consumer_widget")
public class ConsumerWidget {

    @Id
    private UUID id;

    private String name;

    protected ConsumerWidget() {
    }

    public ConsumerWidget(UUID id, String name) {
        this.id = id;
        this.name = name;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
