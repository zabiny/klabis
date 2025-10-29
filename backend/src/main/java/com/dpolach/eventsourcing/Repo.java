package com.dpolach.eventsourcing;

import com.dpolach.inmemoryrepository.InMemoryRepository;
import org.springframework.stereotype.Repository;

@Repository
interface Repo extends InMemoryRepository<BaseEvent, Long> {
}
