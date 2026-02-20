package com.ellipsis.homelabmonitor.kafka;

import com.ellipsis.homelabmonitor.event.ContainerEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class ContainerEventProducer {

    private static final String TOPIC = "container-events";
    private final KafkaTemplate<String, ContainerEvent> kafkaTemplate;

    public ContainerEventProducer(KafkaTemplate<String, ContainerEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishEvent(ContainerEvent event) {
        try {
            kafkaTemplate.send(TOPIC, event.getContainerName(), event)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            System.out.println("Kafka unavailable, skipping event for: "
                                    + event.getContainerName());
                        } else  {
                            System.out.println("Published event: " + event.getEventType()
                                + " for " + event.getContainerName());
                        }
                    });
        } catch (Exception e) {
            System.out.println("Kafka unavailable, skipping event: " + e.getMessage());
        }
    }
}
