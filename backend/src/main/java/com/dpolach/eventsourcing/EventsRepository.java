package com.dpolach.eventsourcing;

import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EventsRepository extends InMemoryRepository<BaseEvent, Long> {
}
